package qbit

import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.model.EID


class EIDTest {

    @Test
    fun testLongConstructor() {
        val eidByte = EID(Byte.MAX_VALUE.toLong())
        assertEquals(0, eidByte.iid)
        assertEquals(Byte.MAX_VALUE.toInt(), eidByte.eid)


        val eidShort = EID(Short.MAX_VALUE.toLong())
        assertEquals(0, eidShort.iid)
        assertEquals(Short.MAX_VALUE.toInt(), eidShort.eid)


        val eidInt = EID(Int.MAX_VALUE.toLong())
        assertEquals(0, eidInt.iid)
        assertEquals(Int.MAX_VALUE, eidInt.eid)
    }

}