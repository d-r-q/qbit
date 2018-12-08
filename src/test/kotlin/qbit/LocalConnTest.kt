package qbit

import org.junit.Assert.*
import org.junit.Test
import qbit.ns.Namespace
import qbit.schema.RefAttr
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

    @Test
    fun testPersistRef() {
        val user = Namespace("user")
        val _val = ScalarAttr(user["val"], QString)
        val _ref = RefAttr(user["ref"])

        val conn = qbit(MemStorage())
        conn.persist(_val, _ref)

        val e1 = Entity(_val to "e1")
        val e2 = Entity(_val to "e2", _ref to e1)

        conn.persist(e1, e2)
        val se2 = conn.db.query(attrIs(_val, "e2")).toList()[0]
        val se1 = se2[_ref]!!
        assertEquals("e1", se1[_val])
    }

    @Test
    fun testPersistRefCycle() {
        val user = Namespace("user")
        val _val = ScalarAttr(user["val"], QString)
        val _ref = RefAttr(user["ref"])

        val conn = qbit(MemStorage())
        conn.persist(_val, _ref)

        var e1 = Entity(_val to "e1")
        e1 = conn.persist(e1).second
        val e2 = Entity(_val to "e2", _ref to e1)
        val e3 = Entity(_val to "e3", _ref to e2)
        e1 = e1.set(_ref, e3)

        conn.persist(e1, e2, e3)

        val se1 = conn.db.query(attrIs(_val, "e1")).toList()[0]
        assertEquals("e1", se1[_val])
        assertEquals("e3", se1[_ref]!![_val])
        assertEquals("e2", se1[_ref]!![_ref]!![_val])
        val se3 = conn.db.query(attrIs(_val, "e3")).toList()[0]
        assertEquals("e3", se3[_val])
        assertEquals("e2", se3[_ref]!![_val])
        assertEquals("e1", se3[_ref]!![_ref]!![_val])
    }

    @Test
    fun testRootOnlyPersist() {
        val user = Namespace("user")
        val _val = ScalarAttr(user["val"], QString)
        val _ref = RefAttr(user["ref"])

        val conn = qbit(MemStorage())
        conn.persist(_val, _ref)

        val e1 = Entity(_val to "e1")
        val e2 = Entity(_val to "e2", _ref to e1)
        val e3 = Entity(_val to "e3", _ref to e2)

        conn.persist(e3)

        val se3 = conn.db.query(attrIs(_val, "e3")).toList()[0]
        assertEquals("e3", se3[_val])
        assertEquals("e2", se3[_ref]!![_val])
        assertEquals("e1", se3[_ref]!![_ref]!![_val])
        se3[_ref]
    }
}


