package qbit.collections

private const val DEFAULT_DEGREE = 1024

fun <E : Any> BTree(cmp: Comparator<E>, degree: Int = DEFAULT_DEGREE): BTree<E> = Leaf(arrayListOf(), degree, cmp, true)

sealed class BTree<E : Any>(
        internal val items: ArrayList<E>,
        val degree: Int,
        val cmp: Comparator<E>,
        val height: Int,
        val root: Boolean
) : Set<E> {

    internal val minItems = degree / 2

    abstract fun add(value: E): BTree<E>

    fun addAll(vararg values: E): BTree<E> =
            addAll(values.asList())

    fun addAll(values: Iterable<E>): BTree<E> {
        val trees = addAllImpl(values.asSequence().distinct().sortedWith(cmp).asIterable())
        val root = when (trees.size) {
            0 -> throw AssertionError("addAllImpl returned empty list")
            1 -> trees[0]
            else -> {
                val children = trees.map { it.withRoot(false) } as ArrayList<BTree<E>>
                val root = spread(children, trees[0].height + 1, true)
                assert(root.size == 1)
                root[0]
            }
        }
        return root
    }

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

    internal abstract fun addAllImpl(values: Iterable<E>): ArrayList<BTree<E>>

    internal abstract fun withRoot(root: Boolean): BTree<E>

    protected fun spread(children: ArrayList<BTree<E>>, height: Int, canGrow: Boolean): ArrayList<BTree<E>> {
        val items = children.subList(1).map { it.first() } as ArrayList<E>
        if (items.size <= degree) {
            return arrayListOf(Node(items, children, degree, cmp, height, root))
        }

        val chunkSize = (minItems + degree) / 2
        val nChildren = split(children, chunkSize, minItems + 1, degree + 1)
        val nItems = arrayListOf<ArrayList<E>>()
        var idx = 0
        for (child in nChildren) {
            nItems.add(ArrayList(items.subList(idx, idx + child.size - 1)))
            idx += child.size
        }
        check(nChildren.size == nItems.size)
        val nodes: ArrayList<BTree<E>> = nChildren.asSequence()
                .zip(nItems.asSequence())
                .map { Node(it.second, it.first, degree, cmp, height, false) }
                .toCollection(ArrayList())

        return if (canGrow) {
            spread(nodes, height + 1, canGrow)
        } else {
            ArrayList(nodes)
        }
    }

}

class Node<E : Any>(
        separators: ArrayList<E>,
        internal val children: ArrayList<BTree<E>>,
        degree: Int,
        cmp: Comparator<E>,
        height: Int,
        root: Boolean
) : BTree<E>(separators, degree, cmp, height, root) {

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

    override fun add(value: E): BTree<E> =
            addAll(value)

    override fun addAllImpl(values: Iterable<E>): ArrayList<BTree<E>> {
        val childValues = splitByArray(values, items, cmp)
        val nItems = ArrayList<E>()
        val nChildren = ArrayList<BTree<E>>()
        for (i in 0 until children.size) {
            if (childValues[i].size > 0) {
                val reses = children[i].addAllImpl(childValues[i])
                for (res in reses) {
                    if (res is Node) {
                        when {
                            res.height == height -> {
                                nItems.addAll(res.items)
                                nChildren.addAll(res.children)
                            }
                            res.height == height - 1 -> nChildren.add(res)
                            else -> throw AssertionError("Should never happen")
                        }
                    } else {
                        nChildren.add(res)
                    }
                }
            } else {
                nChildren.add(children[i])
            }
            if (i < items.size) {
                nItems.add(items[i])
            }
        }

        return spread(nChildren, height, false)
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
            return Node(items, nChildren, degree, cmp, height, root)
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
            Node(nItms, nChildren, degree, cmp, height, root)
        } else {
            val (nLeft, nRight) = rotate(left, right, items[lIdx])
            val nItms = replace(items, nRight.first(), lIdx)
            val nChildren = replaceAll(children, listOf(nLeft, nRight), lIdx, rIdx)
            Node(nItms, nChildren, degree, cmp, height, root)
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
                    Node(nItms, ArrayList(left.children + right.children), degree, cmp, height - 1, left.root)
                }
                left is Leaf<E> && right is Leaf<E> ->
                    Leaf(ArrayList(left.items + right.items), degree, cmp, left.root)
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
                        val nLeft = Node(lItms, lChildren, left.degree, left.cmp, left.height, false)
                        val nRight = Node(rItms, rChildren, right.degree, right.cmp, right.height, false)
                        Pair(nLeft, nRight)
                    } else {
                        val lItms = remove(left.items, left.items.size - 1)
                        val lChildren = remove(left.children, left.children.size - 1)
                        val rItms = insert(right.items, splitter, 0)
                        val rChildren = insert(right.children, left.children.last(), 0)
                        val nLeft = Node(lItms, lChildren, left.degree, left.cmp, left.height, false)
                        val nRight = Node(rItms, rChildren, right.degree, right.cmp, right.height, false)
                        Pair(nLeft, nRight)
                    }
                }
                left is Leaf<E> && right is Leaf<E> -> {
                    val (lItms, rItms) = splitToPair((left.items + right.items) as ArrayList)
                    Pair(Leaf(lItms, degree, cmp, left.root), Leaf(rItms, degree, cmp, right.root))
                }
                else ->
                    throw AssertionError("Should never happen")
            }

    override fun withRoot(root: Boolean) =
            Node(this.items, this.children, this.degree, this.cmp, this.height, root)

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

class Leaf<E : Any>(values: ArrayList<E>, degree: Int, cmp: Comparator<E>, root: Boolean) : BTree<E>(values, degree, cmp, 1, root) {
    override val size = values.size

    override fun add(value: E): BTree<E> {
        return addAll(value)
    }

    override fun addAllImpl(values: Iterable<E>): ArrayList<BTree<E>> {
        val newValues = selectNew(values)
        if (newValues.size == 0) {
            return arrayListOf(this)
        }
        val nItms = merge(items, newValues, cmp)

        return if (nItms.size <= degree) {
            arrayListOf(Leaf(nItms, degree, cmp, root))
        } else {
            val itms = split(nItms, minItems, minItems, degree)
            val children = itms.map { Leaf(it, degree, cmp, false) as BTree<E> } as ArrayList<BTree<E>>
            spread(children, height + 1, false)
        }
    }

    private fun selectNew(values: Iterable<E>): ArrayList<E> {
        return values.filter { items.binarySearch(it, cmp) < 0 } as ArrayList<E>
    }

    override fun remove(value: E): BTree<E> {
        val idx = items.binarySearch(value, cmp)
        return if (idx < 0) {
            this
        } else {
            Leaf(items.filter { cmp.compare(value, it) != 0 } as ArrayList<E>, degree, cmp, root)
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

    override fun withRoot(root: Boolean): BTree<E> =
            Leaf(items, degree, cmp, root)

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
