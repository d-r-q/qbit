package qbit.collections

import java.util.Collections.singleton
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

    fun remove(value: E): BTree<E> =
            removeAll(singleton(O(value))).first

    inner class O(val e: E) : Selector<E, O> {
        override fun invoke(p1: E): Int {
            return cmp.compare(p1, e)
        }

        override fun compareTo(other: O): Int {
            return cmp.compare(e, other.e)
        }
    }

    fun <S : Selector<E, S>> removeAll(selectors: Iterable<S>): Pair<BTree<E>, Iterable<E>> {
        val (root, removed) = removeAllImpl(selectors)
        return finishTree(arrayListOf(root)) to removed
    }

    internal abstract fun <S : Selector<E, S>> removeAllImpl(selectors: Iterable<S>): Pair<BTree<E>, Iterable<E>>

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

    /**
     * Builds treetop for given list of trees and checks invariants.
     */
    private fun finishTree(trees: ArrayList<BTree<E>>): BTree<E> {
        assert(trees.isNotEmpty())
        val root = if (trees.size == 1) {
            pullTillValid(trees[0])
        } else {
            spreadTillValid(trees, trees[0].height + 1)
        }
        root.assertInvariants()
        check(root.root)
        return root
    }

    internal abstract fun addAllImpl(values: Iterable<E>): ArrayList<BTree<E>>

    internal abstract fun withRoot(root: Boolean): BTree<E>

    /**
     * Cuts skinny (nodes with single child) treetop of given tree
     */
    private fun pullTillValid(tree: BTree<E>): BTree<E> {
        var res = tree
        while (res is Node && res.children.size == 1) {
            res = res.children[0]
        }
        return res.withRoot(true)
    }

    /**
     * Builds tree top for given list of trees
     */
    private fun spreadTillValid(trees: ArrayList<BTree<E>>, height: Int): BTree<E> {
        if (trees.size == 1) {
            check(trees[0].items.size <= degree)
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

    protected fun merge(children: ArrayList<BTree<E>>): ArrayList<BTree<E>> {
        val loadedChildren = ArrayList<BTree<E>>()
        loadedChildren.add(children[0])
        children.asSequence().drop(1).forEach {
            if (loadedChildren.last().items.size >= minItems) {
                loadedChildren.add(it)
            } else {
                val left = loadedChildren.removeAt(loadedChildren.size - 1)
                val merge = left.items.size + it.items.size + 1 <= degree
                if (merge) {
                    val m = qbit.collections.merge(left, it)
                    loadedChildren.add(m)
                } else {
                    val (nLeft, nRight) = rotate(left, it)
                    loadedChildren.addAll(listOf(nLeft, nRight))
                }
            }
        }
        if (loadedChildren.size > 1 && loadedChildren.last().items.size < minItems) {
            val right = loadedChildren.removeAt(loadedChildren.size - 1)
            val left = loadedChildren.removeAt(loadedChildren.size - 1)
            val merge = left.items.size + right.items.size + 1 <= degree
            if (merge) {
                val m = qbit.collections.merge(left, right)
                loadedChildren.add(m)
            } else {
                val (nLeft, nRight) = rotate(left, right)
                loadedChildren.addAll(listOf(nLeft, nRight))
            }
        }
        return loadedChildren
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
        val valuesForChild = splitByArray(values, items) { e1, e2 -> cmp.compare(e1, e2) }
        val nChildren = children
                .zip(valuesForChild)
                .flatMap { (c, v) -> c.addAllImpl(v) }
        return spread(nChildren as ArrayList<BTree<E>>, height)
    }

    override fun <S : Selector<E, S>> removeAllImpl(selectors: Iterable<S>): Pair<BTree<E>, Iterable<E>> {
        val removed = ArrayList<E>()
        val nChildren = ArrayList(children)
        val ss = selectors.toList()
        var cIdx = 0
        var sIdx = 0
        while (cIdx < children.size && sIdx < ss.size) {
            val r = if (cIdx < items.size) {
                ss[sIdx].invoke(items[cIdx])
            } else {
                ss[sIdx].invoke(children.last().last())
            }
            when {
                r > 0 -> {
                    val (nChild, cRemoved) = nChildren[cIdx].removeAllImpl(singleton(ss[sIdx]))
                    nChildren[cIdx] = nChild
                    removed.addAll(cRemoved)
                    sIdx++
                }
                r == 0 -> {
                    val (nChild, cRemoved) = nChildren[cIdx].removeAllImpl(singleton(ss[sIdx]))
                    nChildren[cIdx] = nChild
                    removed.addAll(cRemoved)
                    cIdx++
                    if (cIdx < nChildren.size) {
                        val (nChild2, cRemoved2) = nChildren[cIdx].removeAllImpl(singleton(ss[sIdx]))
                        nChildren[cIdx] = nChild2
                        removed.addAll(cRemoved2)
                    }
                }
                r < 0 -> {
                    cIdx++
                }
            }
        }
        val mergedChildren = merge(nChildren)
        val nItems = mergedChildren.drop(1).map { it.first() } as ArrayList<E>
        return Pair(Node(nItems, mergedChildren, degree, cmp, height, root), removed)
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
            val (nLeft, nRight) = rotate(left, right)
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

    internal fun findChildFor(k: E): Int {
        val itemIdx = items.binarySearch(k, cmp)
        return when {
            itemIdx < 0 -> -itemIdx - 1
            else -> itemIdx + 1
        }
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

private fun <E : Any> rotate(left: BTree<E>, right: BTree<E>): Pair<BTree<E>, BTree<E>> =
        when {
            left is Node<E> && right is Node<E> -> {
                val (nlChildren, nrChildren) = splitToPair((left.children + right.children) as ArrayList<BTree<E>>)

                val nlItems = nlChildren.asSequence().drop(1).map { it.first() }.toCollection(ArrayList(nlChildren.size - 1))
                val nLeft = Node(nlItems, nlChildren, left.degree, left.cmp, left.height, false)

                val nrItems = nrChildren.asSequence().drop(1).map { it.first() }.toCollection(ArrayList(nrChildren.size - 1))
                val nRight = Node(nrItems, nrChildren, right.degree, right.cmp, right.height, false)

                Pair(nLeft, nRight)
            }
            left is Leaf<E> && right is Leaf<E> -> {
                val (lItms, rItms) = splitToPair((left.items + right.items) as ArrayList)
                Pair(Leaf(lItms, left.degree, left.cmp, left.root), Leaf(rItms, left.degree, left.cmp, right.root))
            }
            else ->
                throw AssertionError("Should never happen")
        }

private fun <E : Any> merge(left: BTree<E>, right: BTree<E>): BTree<E> =
        when {
            left is Node<E> && right is Node<E> -> {
                val nChildren = left.children
                val rIter = right.children.iterator()
                if (nChildren.last().items.size < left.minItems) {
                    nChildren[nChildren.size - 1] = merge(nChildren.last(), rIter.next())
                }
                nChildren.addAll(rIter.asSequence())
                val nItms = nChildren.drop(1).map { it.first() }
                Node(nItms as ArrayList<E>, nChildren, left.degree, left.cmp, left.height, left.root)
            }
            left is Leaf<E> && right is Leaf<E> ->
                Leaf(ArrayList(left.items + right.items), left.degree, left.cmp, left.root)
            else ->
                throw AssertionError("Should never happen")
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

    override fun <S : Selector<E, S>> removeAllImpl(selectors: Iterable<S>): Pair<BTree<E>, Iterable<E>> {
        val cleanedItms = ArrayList(items)
        val removed = ArrayList<E>()
        var prevSelector: S? = null
        for (selector in selectors) {
            check(prevSelector == null || prevSelector < selector)
            prevSelector = selector
            val fromIdx = cleanedItms.firstMatchIdx(selector)
            if (fromIdx >= 0 && fromIdx < cleanedItms.size) {
                val subItmsIter = cleanedItms.subList(fromIdx, cleanedItms.size).iterator()
                while (subItmsIter.hasNext()) {
                    val item = subItmsIter.next()
                    if (selector.invoke(item) == 0) {
                        removed.add(item)
                        subItmsIter.remove()
                    } else {
                        break
                    }
                }
            }

        }

        return Pair(Leaf(cleanedItms, degree, cmp, root), removed)
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

