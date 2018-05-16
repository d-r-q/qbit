package qbit.collections

private const val DEFAULT_DEGREE = 1024

fun <E : Any> BTree(cmp: Comparator<E>, degree: Int = DEFAULT_DEGREE): BTree<E> = Leaf(arrayListOf(), degree, cmp)

sealed class BTree<E : Any>(
        internal val items: ArrayList<E>,
        val degree: Int,
        val cmp: Comparator<E>
) : Set<E> {

    internal val minItems = degree / 2

    abstract fun add(value: E): BTree<E>

    abstract fun remove(value: E): BTree<E>

    abstract fun find(c: (E) -> Int): Iterator<E>

    abstract fun first(): E

    abstract fun last(): E

    abstract override val size: Int

    abstract override fun contains(element: E): Boolean

    abstract override fun iterator(): Iterator<E>

    override fun containsAll(elements: Collection<E>): Boolean =
            elements.all { contains(it) }

    override fun isEmpty(): Boolean =
            size == 0

    fun select(cmp: (E) -> Int) =
            find(cmp).asSequence()
                    .takeWhile { cmp(it) == 0 }
                    .iterator()

}

class Node<E : Any>(
        separators: ArrayList<E>,
        internal val children: ArrayList<BTree<E>>,
        degree: Int, cmp: Comparator<E>
) : BTree<E>(separators, degree, cmp) {

    override val size = children.sumBy { it.size }

    init {
        require(separators.size == children.size - 1)
        assert(sorted(separators, cmp))
        if (heights(this).toSet().size > 1) {
            println("test")
        }
        assert(heights(this).toSet().size == 1)
        assert(children.asSequence().windowed(2).all { cmp.compare(it[0].last(), it[1].first()) < 0 })
        assert(separators.withIndex().all { idx -> cmp.compare(children[idx.index].last(), idx.value) < 0 })
        assert(cmp.compare(separators.last(), children.last().first()) <= 0)
    }

    override fun add(value: E): BTree<E> {
        val childIdx = findChildFor(value)
        val res = children[childIdx].add(value)
        return when {
            res == children[childIdx] -> this
            res is Leaf || res.items.size > 1 -> {
                val nChildren = set(children, res, childIdx)
                Node(items, nChildren, degree, cmp)
            }
            res is Node -> {
                val nItems = insert(items, res.items[0], childIdx)
                val nChildren = replace(children, arrayListOf(res.children[0], res.children[1]), childIdx)
                return if (nItems.size <= degree) {
                    Node(nItems, nChildren, degree, cmp)
                } else {
                    val (lItms, centerItm, rItms) = splitToTriple(nItems)
                    val (lChildren, rChildren) = splitToPair(nChildren)
                    val parentChildren: ArrayList<BTree<E>> = arrayListOf(Node(lItms, lChildren, degree, cmp), Node(rItms, rChildren, degree, cmp))
                    Node(arrayListOf(centerItm), parentChildren, degree, cmp)
                }
            }
            else -> throw AssertionError("Should never happen")
        }
    }

    override fun contains(element: E): Boolean {
        val childIdx = findChildFor(element)
        return children[childIdx].contains(element)
    }

    override fun remove(value: E): BTree<E> {
        val childIdx = findChildFor(value)
        val nChild = children[childIdx].remove(value)

        if (nChild == children[childIdx]) {
            return this
        }

        if (nChild.items.size >= minItems) {
            val nChildren = set(children, nChild, childIdx)
            return Node(items, nChildren, degree, cmp)
        }

        val (lIdx, rIdx) = when {
            childIdx == 0 -> Pair(childIdx, 1)
            childIdx == children.size - 1 -> Pair(children.size - 2, childIdx)
            children[childIdx - 1].size > children[childIdx + 1].size -> Pair(childIdx, childIdx + 1)
            else -> Pair(childIdx - 1, childIdx)
        }

        val (left, right) = when {
            rIdx > childIdx -> Pair(nChild, children[rIdx])
            else -> Pair(children[lIdx], nChild)
        }

        val merge = left.items.size + right.items.size <= degree
        return if (merge) {
            val m = merge(left, right)
            if (children.size == 2) {
                return m
            }

            val nItms = remove(items, lIdx)
            val nChildren = replace(children, m, lIdx, rIdx)
            Node(nItms, nChildren, degree, cmp)
        } else {
            val (nLeft, nRight) = rotate(left, right, items[lIdx])
            val nItms = replace(items, nRight.first(), lIdx)
            val nChildren = replaceAll(children, listOf(nLeft, nRight), lIdx, rIdx)
            Node(nItms, nChildren, degree, cmp)
        }
    }

    internal fun findChildFor(k: E): Int {
        val itemIdx = items.binarySearch(k, cmp)
        return when {
            itemIdx < 0 -> -itemIdx - 1
            else -> itemIdx + 1
        }
    }

    private fun merge(left: BTree<E>, right: BTree<E>): BTree<E> =
            when {
                left is Node<E> && right is Node<E> -> {
                    val nItms = ArrayList(left.items)
                    nItms.add(right.first())
                    nItms.addAll(right.items)
                    Node(nItms, ArrayList(left.children + right.children), degree, cmp)
                }
                left is Leaf<E> && right is Leaf<E> ->
                    Leaf(ArrayList(left.items + right.items), degree, cmp)
                else ->
                    throw AssertionError("Should never happen")
            }

    private fun rotate(left: BTree<E>, right: BTree<E>, splitter: E): Pair<BTree<E>, BTree<E>> =
            when {
                left is Node<E> && right is Node<E> -> {
                    if (left.size < right.size) {
                        val lItms = insert(left.items, splitter, left.items.size)
                        val lChildren = insert(left.children, right.children[0], left.children.size)
                        val rItms = remove(right.items, 0)
                        val rChildren = remove(right.children, 0)
                        val nLeft = Node(lItms, lChildren, left.degree, left.cmp)
                        val nRight = Node(rItms, rChildren, right.degree, right.cmp)
                        Pair(nLeft, nRight)
                    } else {
                        val lItms = remove(left.items, left.items.size - 1)
                        val lChildren = remove(left.children, left.children.size - 1)
                        val rItms = insert(right.items, splitter, 0)
                        val rChildren = insert(right.children, left.children.last(), 0)
                        val nLeft = Node(lItms, lChildren, left.degree, left.cmp)
                        val nRight = Node(rItms, rChildren, right.degree, right.cmp)
                        Pair(nLeft, nRight)
                    }
                }
                left is Leaf<E> && right is Leaf<E> -> {
                    val (lItms, rItms) = splitToPair((left.items + right.items) as ArrayList)
                    Pair(Leaf(lItms, degree, cmp), Leaf(rItms, degree, cmp))
                }
                else ->
                    throw AssertionError("Should never happen")
            }

    override fun find(c: (E) -> Int): Iterator<E> {
        var idx = items.firstMatchIdx(c)
        if (idx < 0) {
            idx = -idx - 1
        }
        return NodeIterator(this, idx, children[idx].find(c))
    }

    override fun first(): E =
            children.first().first()

    override fun last(): E =
            children.last().last()

    override fun iterator(): Iterator<E> =
            NodeIterator(this, 0, children[0].iterator())

}

class Leaf<E : Any>(values: ArrayList<E>, degree: Int, cmp: Comparator<E>) : BTree<E>(values, degree, cmp) {

    override val size = values.size

    override fun add(value: E): BTree<E> {
        val idx = items.binarySearch(value, cmp)
        if (idx >= 0) {
            return this
        }

        val nItms = merge(items, value, cmp)
        return if (nItms.size < degree) {
            Leaf(nItms, degree, cmp)
        } else {
            val (lItms, rItms) = splitToPair(nItms)
            val children: ArrayList<BTree<E>> = arrayListOf(Leaf(lItms, degree, cmp), Leaf(rItms, degree, cmp))
            Node(arrayListOf(rItms[0]), children, degree, cmp)
        }
    }

    override fun remove(value: E): BTree<E> {
        val idx = items.binarySearch(value, cmp)
        return if (idx < 0) {
            this
        } else {
            Leaf(ArrayList(items.filter { cmp.compare(value, it) != 0 }), degree, cmp)
        }
    }

    override fun contains(element: E): Boolean =
            items.indexOf(element) >= 0

    override fun find(c: (E) -> Int): Iterator<E> {
        val idx = items.firstMatchIdx(c)
        return if (idx >= 0) {
            LeafIterator(items, idx)
        } else {
            LeafIterator(items, -idx - 1)
        }
    }

    override fun first(): E =
            items[0]

    override fun last(): E =
            items.last()

    override fun iterator(): Iterator<E> =
            LeafIterator(items, 0)

}

// Iterators

class LeafIterator<out E : Any>(private val values: ArrayList<E>, private var idx: Int) : Iterator<E> {

    override fun hasNext(): Boolean =
            idx < values.size

    override fun next(): E =
            values[idx++]

}

class NodeIterator<out E : Any>(
        private val node: Node<E>,
        private var childIdx: Int,
        private var childIter: Iterator<E>
) : Iterator<E> {

    override fun hasNext(): Boolean =
            childIter.hasNext() || childIdx < node.children.size - 1

    override fun next(): E {
        if (!childIter.hasNext()) {
            childIter = node.children[++childIdx].iterator()
        }
        return childIter.next()
    }

}


// Invariants checking

internal fun heights(n: BTree<*>): List<Int> =
        when (n) {
            is Leaf -> listOf(1)
            is Node -> n.children.flatMap { heights(it).map { it + 1 } }
        }

internal fun assertDegreeInv(n: BTree<*>, root: Boolean) {
    if (!root) {
        assert((n.items.size >= n.minItems))
    }
    if (n is Node) {
        n.children.forEach { assertDegreeInv(it, false) }
    }
}
