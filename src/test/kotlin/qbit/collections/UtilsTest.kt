package qbit.collections

import org.junit.Assert.assertTrue
import org.junit.Test


class UtilsTest {

    @Test
    fun testSplitByArray() {
        val arr = listOf(4, 5, 8, 9)
        val resFirst = splitByArray(arr, listOf(10), naturalOrder())
        assertTrue(resFirst.size == 2)
        assertTrue(resFirst[0].size == 4)
        assertTrue(resFirst[1].size == 0)

        val resLast = splitByArray(arr, listOf(3), naturalOrder())
        assertTrue(resLast.size == 2)
        assertTrue(resLast[0].size == 0)
        assertTrue(resLast[1].size == 4)

        val resMiddle = splitByArray(arr, listOf(3, 10), naturalOrder())
        assertTrue(resMiddle.size == 3)
        assertTrue(resMiddle[0].size == 0)
        assertTrue(resMiddle[1].size == 4)
        assertTrue(resMiddle[2].size == 0)

        val resFlat = splitByArray(arr, listOf(5, 6, 9), naturalOrder())
        assertTrue(resFlat.size == 4)
        assertTrue(resFlat[0].size == 1)
        assertTrue(resFlat[1].size == 1)
        assertTrue(resFlat[2].size == 1)
        assertTrue(resFlat[3].size == 1)

        val resEdges = splitByArray(arr, listOf(6, 7), naturalOrder())
        assertTrue(resEdges.size == 3)
        assertTrue(resEdges[0].size == 2)
        assertTrue(resEdges[1].size == 0)
        assertTrue(resEdges[2].size == 2)
    }
}