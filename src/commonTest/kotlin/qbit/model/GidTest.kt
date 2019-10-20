package qbit.model

import kotlin.test.Test
import kotlin.test.assertEquals


class GidTest {

    @Test
    fun testLongConstructor() {
        val eidByte = Gid(Byte.MAX_VALUE.toLong())
        assertEquals(0, eidByte.iid)
        assertEquals(Byte.MAX_VALUE.toInt(), eidByte.eid)


        val eidShort = Gid(Short.MAX_VALUE.toLong())
        assertEquals(0, eidShort.iid)
        assertEquals(Short.MAX_VALUE.toInt(), eidShort.eid)


        val eidInt = Gid(Int.MAX_VALUE.toLong())
        assertEquals(0, eidInt.iid)
        assertEquals(Int.MAX_VALUE, eidInt.eid)
    }

}