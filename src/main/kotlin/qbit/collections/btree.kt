package qbit.collections

import kotlin.math.abs
import kotlin.math.log
import kotlin.math.roundToInt

private const val DEFAULT_DEGREE = 1024

fun <E : Any> BTree(cmp: Comparator<E>, degree: Int = DEFAULT_DEGREE): BTree<E> = Leaf(arrayListOf(), degree, cmp, true)

typealias QComparator<E> = (E) -> Int

interface Selector<E, S> : QComparator<E>, Comparable<S>

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
        val trees = addAllImpl(values)
        return finishTree(trees)
    }

    fun remove(value: E): BTree<E> {
        val root = removeImpl(value)
        root.assertInvariants()
        check(root.root)
        return root
    }

    internal abstract fun removeImpl(value: E): BTree<E>

    abstract fun find(c: (E) -> Int): Iterator<E>

    abstract fun first(): E

    abstract fun last(): E

    override fun containsAll(elements: Collection<E>): Boolean =
            elements.all { contains(it) }

    override fun isEmpty(): Boolean =
            size == 0

    fun select(cmp: (E) -> Int): Iterator<E> =
            find(cmp).asSequence()
                    .takeWhile { cmp(it) == 0 }
                    .iterator()

    fun <S : Selector<E, S>> replace(es: Iterable<Pair<Selector<E, S>, Iterable<E>>>): Pair<BTree<E>, Iterable<Iterable<E>>> {
        val (trees, removed) = replaceImpl(es)
        val root = finishTree(trees)
        return Pair(root, removed)
    }

    /**
     * Builds treetop for given list of trees and checks invariants.
     */
    private fun finishTree(trees: ArrayList<BTree<E>>): BTree<E> {
        assert(trees.isNotEmpty())
        val root = spreadTillValid(trees, trees[0].height + 1)
        root.assertInvariants()
        check(root.root)
        return root
    }

    internal abstract fun <S : Selector<E, S>> replaceImpl(es: Iterable<Pair<Selector<E, S>, Iterable<E>>>): Pair<ArrayList<BTree<E>>, Iterable<Iterable<E>>>

    internal abstract fun addAllImpl(values: Iterable<E>): ArrayList<BTree<E>>

    internal abstract fun withRoot(root: Boolean): BTree<E>

    /**
     * Builds tree top for given list of trees
     */
    private fun spreadTillValid(trees: ArrayList<BTree<E>>, height: Int): BTree<E> {
        if (trees.size == 1 && trees[0].items.size <= degree) {
            // trees list already contains single valid node
            return trees[0]
        }

        val items = trees.subList(1).map { it.first() } as ArrayList<E>
        if (items.size <= degree) {
            // can build valid node for given trees
            return Node(items, trees, degree, cmp, height, root)
        }


        // build next level of nodes for given trees...
        val parents = spread(trees, height)

        // and try to make root from that level
        return spreadTillValid(parents, height + 1)
    }

    protected fun spread(children: ArrayList<BTree<E>>, height: Int): ArrayList<BTree<E>> {
        val items = children.subList(1).map { it.first() } as ArrayList<E>
        if (items.size <= degree) {
            return arrayListOf(Node(items, children, degree, cmp, height, root))
        }

        // unchecked heuristics, that it's better to keep nodes filled for 3/4
        // I believe, that this decreses frequency of tree rebuilds
        val chunkSize = (minItems + degree) / 2
        // split children such way, that each subset satisfies degree invariant
        val nChildren = split(children, chunkSize, minItems + 1)
        val nItems = arrayListOf<ArrayList<E>>()
        var idx = 0
        for (child in nChildren) {
            nItems.add(ArrayList(items.subList(idx, idx + child.size - 1)))
            idx += child.size
        }
        check(nChildren.size == nItems.size)

        return nChildren.asSequence()
                .zip(nItems.asSequence())
                .map { Node(it.second, it.first, degree, cmp, height, false) }
                .toCollection(ArrayList())
    }

    internal abstract fun assertInvariants()

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

    override fun add(value: E): BTree<E> =
            addAll(value)

    override fun addAllImpl(values: Iterable<E>): ArrayList<BTree<E>> {
        return applyToChildren(values, { e1, e2 -> cmp.compare(e1, e2) }, { c, v -> c.addAllImpl(v) })
    }

    private fun <T : Any> applyToChildren(values: Iterable<T>, splitComparator: (T, E) -> Int, map: (BTree<E>, ArrayList<T>) -> ArrayList<BTree<E>>): ArrayList<BTree<E>> {
        val valuesForChild = splitByArray(values, items, splitComparator)
        val nChildren = children.zip(valuesForChild).flatMap { (c, v) -> map(c, v) }
        return spread(nChildren as ArrayList<BTree<E>>, height)
    }

    override fun contains(element: E): Boolean {
        val childIdx = findChildFor(element)
        return children[childIdx].contains(element)
    }

    override fun removeImpl(value: E): BTree<E> {
        val childIdx = findChildFor(value)
        val nChild = children[childIdx].removeImpl(value)

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

        val merge = left.items.size + right.items.size + 1 <= degree
        val (nItms, nChildren) = if (merge) {
            val m = merge(left, right)
            if (!root && children.size <= minItems) {
                return m
            }

            val nItms = remove(items, lIdx)
            val nChildren = replace(children, m, lIdx, rIdx)
            Pair(nItms, nChildren)
        } else {
            val (nLeft, nRight) = rotate(left, right, items[lIdx])
            val nItms = replace(items, nRight.first(), lIdx)
            val nChildren = replaceAll(children, listOf(nLeft, nRight), lIdx, rIdx)
            Pair(nItms, nChildren)
        }

        return when (nChildren.size) {
            0 -> throw AssertionError("Should never happen")
            1 -> nChildren[0].withRoot(root)
            else -> Node(nItms, nChildren, degree, cmp, height, root)
        }
    }

    override fun <S : Selector<E, S>> replaceImpl(es: Iterable<Pair<Selector<E, S>, Iterable<E>>>): Pair<ArrayList<BTree<E>>, Iterable<Iterable<E>>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
                    if (left.items.size < right.items.size) {
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

    override fun assertInvariants() {
        require(root || children.size > minItems)
        require(root || items.size <= degree)
        require(root || items.size >= minItems)
        require(items.size == children.size - 1)

        // check heavy invariants, used in tests
        qbit.assert { sorted(items, cmp) }
        qbit.assert { heights(this).toSet().size == 1 }
        qbit.assert { children.asSequence().windowed(2).all { cmp.compare(it[0].last(), it[1].first()) < 0 } }
        qbit.assert { items.withIndex().all { idx -> cmp.compare(children[idx.index].last(), idx.value) < 0 } }
        qbit.assert { cmp.compare(items.last(), children.last().first()) <= 0 }
        qbit.assert { abs(height - log(size.toDouble(), ((degree + minItems) / 2).toDouble()).roundToInt()) <= 2 }
        qbit.assert { children.all { it.height < height && !it.root } }

        children.forEach { it.assertInvariants() }
    }
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
            val itms = split(nItms, minItems, minItems)
            val children = itms.map { Leaf(it, degree, cmp, false) as BTree<E> } as ArrayList<BTree<E>>
            children
        }
    }

    private fun selectNew(values: Iterable<E>): ArrayList<E> {
        return values.filter { items.binarySearch(it, cmp) < 0 } as ArrayList<E>
    }

    override fun removeImpl(value: E): BTree<E> {
        val idx = items.binarySearch(value, cmp)
        return if (idx < 0) {
            this
        } else {
            val nItems = ArrayList<E>(items.size - 1)
            nItems.addAll(items.subList(0, idx))
            nItems.addAll(items.subList(idx + 1))
            Leaf(nItems, degree, cmp, root)
        }
    }

    override fun <S : Selector<E, S>> replaceImpl(es: Iterable<Pair<Selector<E, S>, Iterable<E>>>): Pair<ArrayList<BTree<E>>, Iterable<Iterable<E>>> {
        val cleanedItms = ArrayList(items)
        val itemsToAdd = ArrayList<E>()
        val removed = ArrayList<ArrayList<E>>()
        for ((selector, newData) in es) {
            val fromIdx = items.firstMatchIdx(selector)
            removed.add(ArrayList())
            if (fromIdx >= 0 && fromIdx < items.size) {
                val subItmsIter = cleanedItms.subList(fromIdx, cleanedItms.size).iterator()
                while (subItmsIter.hasNext()) {
                    val item = subItmsIter.next()
                    if (item == 0) {
                        removed.last().add(item)
                        subItmsIter.remove()
                    } else {
                        break
                    }
                }
            }

            itemsToAdd.addAll(newData)
        }

        val nItms = merge(cleanedItms, itemsToAdd, cmp)

        return if (nItms.size <= degree) {
            Pair(arrayListOf<BTree<E>>(Leaf(nItms, degree, cmp, root)), removed)
        } else {
            val itms = split(nItms, minItems, minItems)
            val children = itms.map { Leaf(it, degree, cmp, false) as BTree<E> } as ArrayList<BTree<E>>
            Pair(spread(children, height + 1), removed)
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

    override fun assertInvariants() {
        require(items.size <= degree)
        require(root || items.size >= minItems)
    }

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
            is Node -> n.children.flatMap { bTree -> heights(bTree).map { it + 1 } }
        }

