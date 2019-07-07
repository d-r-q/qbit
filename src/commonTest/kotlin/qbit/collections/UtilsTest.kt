package qbit.collections

import qbit.assertArrayEquals
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue


class UtilsTest {

    @Test
    fun testSplitByArray() {
        val cmp = { i1: Int, i2: Int -> i1.compareTo(i2) }
        val arr = listOf(4, 5, 8, 9)
        val resFirst = splitByArray(arr, listOf(10), cmp)
        assertTrue(resFirst.size == 2)
        assertTrue(resFirst[0].size == 4)
        assertTrue(resFirst[1].size == 0)

        val resLast = splitByArray(arr, listOf(3), cmp)
        assertTrue(resLast.size == 2)
        assertTrue(resLast[0].size == 0)
        assertTrue(resLast[1].size == 4)

        val resMiddle = splitByArray(arr, listOf(3, 10), cmp)
        assertTrue(resMiddle.size == 3)
        assertTrue(resMiddle[0].size == 0)
        assertTrue(resMiddle[1].size == 4)
        assertTrue(resMiddle[2].size == 0)

        val resFlat = splitByArray(arr, listOf(5, 6, 9), cmp)
        assertTrue(resFlat.size == 4)
        assertTrue(resFlat[0].size == 1)
        assertTrue(resFlat[1].size == 1)
        assertTrue(resFlat[2].size == 1)
        assertTrue(resFlat[3].size == 1)

        val resEdges = splitByArray(arr, listOf(6, 7), cmp)
        assertTrue(resEdges.size == 3)
        assertTrue(resEdges[0].size == 2)
        assertTrue(resEdges[1].size == 0)
        assertTrue(resEdges[2].size == 2)
    }

    @Test
    fun splitSmokeTest() {
        val rnd = Random(1)
        for (i in 0..10000) {
            val size = rnd.nextInt(10000)
            val minChunk = 1 + rnd.nextInt(1 + size / 2)
            val maxChunk = minChunk * 2
            val chunk = minChunk + rnd.nextInt(maxChunk - minChunk)
            val lst = arrayList(size) { true }
            val sublists = split(lst, chunk, minChunk)
            sublists.forEach {
                assertTrue(it.size >= minChunk)
                assertTrue(it.size < maxChunk)
            }
            val (regular, irregular) = sublists.asSequence().partition { it.size == chunk }
            assertTrue(regular.size >= irregular.size || sublists.size <= 3)
        }
    }

    @Test
    fun setTest() {
        val list = arrayListOf(1, 2, 3)
        val new = set(list, 123, 1)
        assertTrue(new[1] == 123)
        assertTrue(new.size == 3)
    }

    @Test
    fun insertTest() {
        val list = arrayListOf(1, 2, 3, 4)
        val expected = arrayListOf(1, 2, 5, 6, 3, 4)
        val newList = insert(list, arrayListOf(5, 6), 2)
        assertArrayEquals(expected.toTypedArray(), newList.toTypedArray())
    }
}