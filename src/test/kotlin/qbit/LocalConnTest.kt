package qbit

import org.junit.Assert.*
import org.junit.Test
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
        val e = mapOf("attr" to "value")
        val (db, eid) = conn.create(e)
        conn.addEntity(eid, mapOf("attr" to "value2"))
        val pulledE2 = conn.db.pull(eid)
        assertEquals("value2", pulledE2!!["attr"] as String)
    }

}

