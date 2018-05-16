package qbit

import org.junit.Assert.*
import org.junit.Test
import qbit.collections.*

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
        val first: BTree<Int> = Leaf(arrayListOf(1, 2, 3, 4), 4, naturalOrder())
        val second = Leaf(arrayListOf(6, 7, 8, 9), 4, naturalOrder())
        val third = Leaf(arrayListOf(10, 11, 12, 13), 4, naturalOrder())
        val fourth = Leaf(arrayListOf(14, 15, 16, 17), 4, naturalOrder())
        val fifths = Leaf(arrayListOf(18, 19, 20, 21), 4, naturalOrder())
        val root = Node(arrayListOf(6, 10, 14, 18), arrayListOf(first, second, third, fourth, fifths), 4, Comparator.naturalOrder())
        val newRoot = root.add(5)
        assert((newRoot as qbit.collections.Node<Int>).children.size == 2)
    }

    @Test
    fun testLeftLeafUnderFlow() {
        val left: BTree<Int> = Leaf(arrayListOf(1, 2, 3, 4), 4, naturalOrder())
        val right = Leaf(arrayListOf(6, 7, 8, 9), 4, naturalOrder())
        val root = Node(arrayListOf(6), arrayListOf(left, right), 4, Comparator.naturalOrder())
        val newRoot = root.remove(4)
        assertEquals(7, newRoot.size)
    }

    @Test
    fun testFindChildFor() {
        val left = Leaf(arrayListOf(1, 2, 3, 4), 4, naturalOrder())
        val right = Leaf(arrayListOf(6, 7, 8, 9), 4, naturalOrder())
        val third = Leaf(arrayListOf(10, 11, 12, 13), 4, naturalOrder())
        val root = Node(arrayListOf(6, 10), arrayListOf<BTree<Int>>(left, right, third), 4, Comparator.naturalOrder())
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
}
