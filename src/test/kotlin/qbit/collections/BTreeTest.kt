package qbit.collections

import org.junit.Assert.*
import org.junit.Test

class BTreeTest {

    @Test
    fun testInsertEls() {
        val origin = arrayListOf<Int>()
        val a1 = merge(origin, arrayListOf(3, 4), naturalOrder())
        assertArrayEquals(arrayOf(3, 4), a1.toArray())
        val a2 = merge(a1, arrayListOf(1, 2), naturalOrder())
        assertArrayEquals(arrayOf(1, 2, 3, 4), a2.toArray())
        val a3 = merge(a2, arrayListOf(5, 6), naturalOrder())
        assertArrayEquals(arrayOf(1, 2, 3, 4, 5, 6), a3.toArray())
        val a4 = merge(arrayListOf(1, 4, 5), arrayListOf(0, 2, 3, 6, 7), naturalOrder())
        assertArrayEquals(arrayOf(0, 1, 2, 3, 4, 5, 6, 7), a4.toArray())
    }

    @Test
    fun testInternalOverflow() {
        val first: BTree<Int> = Leaf(arrayListOf(1, 2, 3, 4), 4, naturalOrder(), false)
        val second = Leaf(arrayListOf(6, 7, 8, 9), 4, naturalOrder(), false)
        val third = Leaf(arrayListOf(10, 11, 12, 13), 4, naturalOrder(), false)
        val fourth = Leaf(arrayListOf(14, 15, 16, 17), 4, naturalOrder(), false)
        val fifths = Leaf(arrayListOf(18, 19, 20, 21), 4, naturalOrder(), false)
        val root = Node(arrayListOf(6, 10, 14, 18), arrayListOf(first, second, third, fourth, fifths), 4, Comparator.naturalOrder(), 2, true)
        val newRoot = root.add(5)
        assertEquals((newRoot as qbit.collections.Node<Int>).children.size, 2)
    }

    @Test
    fun testLeftLeafUnderflow() {
        val left: BTree<Int> = Leaf(arrayListOf(1, 2, 3, 4), 4, naturalOrder(), false)
        val right = Leaf(arrayListOf(6, 7, 8, 9), 4, naturalOrder(), false)
        val root = Node(arrayListOf(6), arrayListOf(left, right), 4, Comparator.naturalOrder(), 2, true)
        val newRoot = root.remove(4)
        assertEquals(7, newRoot.size)
    }

    @Test
    fun testLeafMultipleOverflow() {
        val leaf = Leaf(arrayListOf(0, 1, 2), 4, naturalOrder(), true)
        val node = leaf.addAll(3..1024)
        assertTrue(node is Node)
        assertArrayEquals(arrayOf(486), (node as Node).items.toArray())
    }


    @Test
    fun testNodeMultipleOverflow() {
        val firstNode = with(BTree<Int>(naturalOrder(), 4)) {
            var fn = this
            for (i in 0..3) {
                fn = fn.add(i)
            }
            fn
        }
        val newRoot = firstNode.addAll(4..1024)
        assertTrue(newRoot is Node)
        assertArrayEquals(arrayOf(486), (newRoot as Node).items.toArray())
    }

    @Test
    fun testFindChildFor() {
        val left = Leaf(arrayListOf(1, 2, 3, 4), 4, naturalOrder(), false)
        val right = Leaf(arrayListOf(6, 7, 8, 9), 4, naturalOrder(), false)
        val third = Leaf(arrayListOf(10, 11, 12, 13), 4, naturalOrder(), false)
        val root = Node(arrayListOf(6, 10), arrayListOf<BTree<Int>>(left, right, third), 4, Comparator.naturalOrder(), 2, true)
        assertEquals(0, root.findChildFor(0))
        assertEquals(0, root.findChildFor(1))
        assertEquals(0, root.findChildFor(4))
        assertEquals(0, root.findChildFor(5))
        assertEquals(1, root.findChildFor(6))
        assertEquals(1, root.findChildFor(9))
        assertEquals(2, root.findChildFor(10))
        assertEquals(2, root.findChildFor(13))
        assertEquals(2, root.findChildFor(14))
    }

    @Test
    fun testReplace() {
        val leaf = Leaf(arrayListOf(1, 2, 3, 4), 4, naturalOrder(), false)
        val (added, removed) = leaf.replace(listOf(Pair(IntSelector(5, 5), listOf(5))))
        assertEquals(5, added.size)
        assertEquals(0, removed.toList()[0].toList().size)
        assertArrayEquals(arrayOf(1, 2, 3, 4, 5), added.toTypedArray())
    }

    class IntSelector(private val from: Int, val to: Int) : Selector<Int, IntSelector> {

        override fun invoke(p1: Int): Int {
            return when {
                p1 < from -> -1
                p1 > to -> 1
                else -> 0
            }
        }

        override fun compareTo(other: IntSelector): Int {
            return when {
                from < other.from -> -1
                from > other.from -> -1
                else -> 0
            }
        }

    }
}
