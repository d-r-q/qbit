package qbit

import org.junit.Assert.*
import org.junit.Test
import qbit.ns.Namespace
import qbit.schema.Attr
import qbit.schema.ScalarAttr
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
        val _attr = ScalarAttr(Namespace("user")["attr"], QString)
        conn.persist(_attr)
        val e = Entity(_attr to "value")
        var (_, se) = conn.persist(e)
        se = se.set(_attr, "value2")
        conn.persist(se)
        val pulledE2 = conn.db.pull(se.eid)
        assertEquals("value2", pulledE2!![_attr] as String)
    }

    @Test
    fun testUnique() {
        val conn = qbit(MemStorage())
        val _uid = ScalarAttr(Namespace("user")["uid"], QLong, true)
        conn.persist(_uid)
        conn.persist(Entity(_uid to 0L))
        try {
            conn.persist(Entity(_uid to 0L))
            fail("QBitException expected")
        } catch (e: QBitException) {
            assertTrue(e.message?.contains("Duplicate") ?: false)
        }
    }

    @Test
    fun testDelete() {
        val conn = qbit(MemStorage())
        val _attr = ScalarAttr(Namespace("user")["attr"], QString)
        conn.persist(_attr)

        val e = Entity(_attr to "value")
        var (_, se) = conn.persist(e)
        se = se.set(_attr, "value2")
        conn.persist(se)

        val pulledE2 = conn.db.pull(se.eid)
        assertEquals("value2", pulledE2!![_attr] as String)

        val deleted = pulledE2.delete()
        conn.persist(deleted)

        val deletedPulledE2 = conn.db.pull(se.eid)
        assertNull(deletedPulledE2)
        assertEquals(0, conn.db.query(attrIs(_attr, "value")).size)
        assertEquals(0, conn.db.query(attrIs(_attr, "value2")).size)
    }
}

