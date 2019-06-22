package qbit

import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.model.IID

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

    @Test(expected = QBitException::class)
    fun testOverflow() {
        IID(0, 4).fork(16)
    }


    @Test(expected = QBitException::class)
    fun testNegative() {
        IID(0, 4).fork(0)
    }
}