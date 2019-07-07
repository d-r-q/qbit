package qbit.model

import qbit.QBitException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IIDTest {

    @Test
    fun test1lvlFork() {
        val iid = IID(0, 4)
        assertEquals(0b0001, iid.fork(1).value)
        assertEquals(0b0010, iid.fork(2).value)
        assertEquals(0b0011, iid.fork(3).value)
    }

    @Test
    fun test2lvlFork() {
        val iid = IID(0b0001, 4)
        assertEquals(0b0001_0001, iid.fork(1).value)
        assertEquals(0b0010_0001, iid.fork(2).value)
        assertEquals(0b0011_0001, iid.fork(3).value)
    }

    @Test
    fun testOverflow() {
        assertFailsWith<QBitException> {
            IID(0, 4).fork(16)
        }
    }


    @Test
    fun testNegative() {
        assertFailsWith<QBitException> {
            IID(0, 4).fork(0)
        }
    }
}