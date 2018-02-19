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
        val _attr = Attr(Namespace("user")["attr"], QString)
        conn.create(_attr)
        val e = Entity(_attr to "value")
        val (_, eid) = conn.create(e)
        conn.addEntity(eid, Entity(_attr to "value2"))
        val pulledE2 = conn.db.pull(eid)
        assertEquals("value2", pulledE2!![_attr] as String)
    }

    @Test
    fun testUnique() {
        val conn = qbit(MemStorage())
        val _uid = Attr(Namespace("user")["uid"], QLong, true)
        conn.create(_uid)
        conn.create(Entity(_uid to 0L))
        try {
            conn.create(Entity(_uid to 0L))
            fail("QBitException expected")
        } catch (e: QBitException) {
            assertTrue(e.message?.contains("Duplicate") ?: false)
        }
    }

}

