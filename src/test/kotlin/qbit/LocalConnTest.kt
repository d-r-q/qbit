package qbit

import org.junit.Assert.*
import org.junit.Test
import qbit.schema.Attr
import qbit.storage.MemStorage

class LocalConnTest {

    @Test
    fun testInit() {
        val db = qbit(MemStorage())
        assertNotNull(db)
    }

    @Test
    fun testUpdate() {
        val conn = qbit(MemStorage())
        val e = mapOf("user/attr" to "value")
        conn.create(Attr(Namespace("user")["attr"], QString))
        val (_, eid) = conn.create(e)
        conn.addEntity(eid, mapOf("user/attr" to "value2"))
        val pulledE2 = conn.db.pull(eid)
        assertEquals("value2", pulledE2!!["user/attr"] as String)
    }

}

