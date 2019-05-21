package qbit

import org.junit.Assert.*
import org.junit.Test
import qbit.ns.Namespace
import qbit.schema.ListAttr
import qbit.schema.RefAttr
import qbit.schema.ScalarAttr
import qbit.schema.eq
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
        val e = Entity(_attr eq "value")
        var se = conn.persist(e).storedEntity()
        se = se.set(_attr, "value2")
        conn.persist(se)
        val pulledE2 = conn.db.pull(se.eid)
        assertEquals("value2", pulledE2!![_attr])
    }

    @Test
    fun testUnique() {
        val conn = qbit(MemStorage())
        val _uid = ScalarAttr(Namespace("user")["uid"], QLong, true)
        conn.persist(_uid)
        conn.persist(Entity(_uid eq 0L))
        try {
            conn.persist(Entity(_uid eq 0L))
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

        val e = Entity(_attr eq "value")
        var se = conn.persist(e).storedEntity()
        se = se.set(_attr, "value2")
        conn.persist(se)

        val pulledE2 = conn.db.pull(se.eid)
        assertEquals("value2", pulledE2!![_attr])

        val deleted = pulledE2.delete()
        conn.persist(deleted)

        val deletedPulledE2 = conn.db.pull(se.eid)
        assertNull(deletedPulledE2)
        assertEquals(0, conn.db.query(attrIs(_attr, "value")).count())
        assertEquals(0, conn.db.query(attrIs(_attr, "value2")).count())
    }

    @Test
    fun testPersistRef() {
        val user = Namespace("user")
        val _val = ScalarAttr(user["val"], QString)
        val _ref = RefAttr(user["ref"])

        val conn = qbit(MemStorage())
        conn.persist(_val, _ref)

        val e1 = Entity(_val eq "e1")
        val e2 = Entity(_val eq "e2", _ref eq e1)

        conn.persist(e1, e2)
        val se2 = conn.db.query(attrIs(_val, "e2")).toList()[0]
        val se1 = se2[_ref]
        assertEquals("e1", se1[_val])
    }

    @Test
    fun testPersistRefCycle() {
        val user = Namespace("user")
        val _val = ScalarAttr(user["val"], QString)
        val _ref = RefAttr(user["ref"])

        val conn = qbit(MemStorage())
        conn.persist(_val, _ref)

        var e1 = Entity(_val eq "e1")
        e1 = conn.persist(e1).storedEntity()
        val e2 = Entity(_val eq "e2", _ref eq e1)
        val e3 = Entity(_val eq "e3", _ref eq e2)
        e1 = e1.set(_ref, e3)

        conn.persist(e1, e2, e3)

        val se1 = conn.db.query(attrIs(_val, "e1")).toList()[0]
        assertEquals("e1", se1[_val])
        assertEquals("e3", se1[_ref][_val])
        assertEquals("e2", se1[_ref][_ref][_val])
        val se3 = conn.db.query(attrIs(_val, "e3")).toList()[0]
        assertEquals("e3", se3[_val])
        assertEquals("e2", se3[_ref][_val])
        assertEquals("e1", se3[_ref][_ref][_val])
    }

    @Test
    fun testRootOnlyPersist() {
        val user = Namespace("user")
        val _val = ScalarAttr(user["val"], QString)
        val _ref = RefAttr(user["ref"])

        val conn = qbit(MemStorage())
        conn.persist(_val, _ref)

        val e1 = Entity(_val eq "e1")
        val e2 = Entity(_val eq "e2", _ref eq e1)
        val e3 = Entity(_val eq "e3", _ref eq e2)

        conn.persist(e3)

        val se3 = conn.db.query(attrIs(_val, "e3")).toList()[0]
        assertEquals("e3", se3[_val])
        assertEquals("e2", se3[_ref][_val])
        assertEquals("e1", se3[_ref][_ref][_val])
        se3[_ref]
    }

    @Test
    fun testRestoreEntity() {
        val user = Namespace("user")
        val _val = ScalarAttr(user["val"], QString)

        val conn = qbit(MemStorage())
        conn.persist(_val)

        val e1 = Entity(_val eq "e1")
        val se1 = conn.persist(e1).storedEntity()
        val h = conn.head
        conn.persist(se1)
        assertEquals(h, conn.head)
    }

    @Test
    fun testRestoreEntityWithNew() {
        val user = Namespace("user")
        val _val = ScalarAttr(user["val"], QString)
        val _ref = RefAttr(user["ref"])

        val conn = qbit(MemStorage())
        conn.persist(_val, _ref)

        val e1 = Entity(_val eq "e1")
        val se1 = conn.persist(e1).storedEntity()

        val e2 = Entity(_val eq "e1", _ref eq se1)
        conn.persist(e2)

        assertEquals(5, conn.head.data.trx.size)
    }

    @Test
    fun testChangeNotUnqiueAttrWhenUniqueIsPresented() {
        val user = Namespace("user")
        val unique = ScalarAttr(user["unique"], QString, true)
        val notUnique = ScalarAttr(user["not_unique"], QString)

        val conn = qbit(MemStorage())

        conn.persist(unique, notUnique)
        val se1 = conn.persist(Entity(unique eq "unique", notUnique eq "notUnique1")).storedEntity()
        val se2 = conn.persist(se1.set(notUnique, "notUnique2")).storedEntity()
        assertEquals("notUnique2", se2[notUnique])
    }

    @Test
    fun testPersistNotAcutallyChanged() {
        val user = Namespace("user")
        val _val = ScalarAttr(user["val"], QString)

        val conn = qbit(MemStorage())
        conn.persist(_val)

        val e1 = Entity(_val eq "e1")
        val se1 = conn.persist(e1).storedEntity()
        val h = conn.head
        conn.persist(se1.set(_val, "e1"))
        assertEquals(h, conn.head)
    }

    @Test
    fun testEntityRetrievalFromUpdatedEntity() {
        val user = Namespace("user")
        val _val = ScalarAttr(user["val"], QString)
        val _ref = RefAttr(user["ref"])

        val conn = qbit(MemStorage())
        conn.persist(_val, _ref)

        val e1 = Entity(_val eq "e1")
        val e2 = Entity(_val eq "e2", _ref eq e1)
        var (se1, se2) = conn.persist(e1, e2).persistedEntities
        se1 = se1.set(_val, "e1.1")
        se2 = se2.set(_val, "e2.1")

        se2 = conn.persist(se2, se1).storedEntity()
        assertEquals("e1.1", se2[_ref][_val])
    }

    @Test
    fun testPersistEntityWithList() {
        val user = Namespace("user")
        val _id = ScalarAttr(user["val"], QString)
        val _list = ListAttr(user["list"], QString)

        val conn = qbit(MemStorage())
        conn.persist(_id, _list)

        val e = Entity(_id eq "1", _list eq listOf("1", "2"))
        val se = conn.persist(e).storedEntity()

        assertEquals(listOf("1", "2"), se[_list])
    }

    @Test
    fun testPersistenceOfListClearing() {
        val user = Namespace("user")
        val _id = ScalarAttr(user["val"], QString)
        val _list = ListAttr(user["list"], QString)

        val conn = qbit(MemStorage())
        conn.persist(_id, _list)

        val e = Entity(_id eq "1", _list eq listOf("1", "2"))
        var se = conn.persist(e).storedEntity()

        assertEquals(listOf("1", "2"), se[_list])

        se = conn.persist(se.set(_list, emptyList())).storedEntity()
        assertNull(se.getO(_list))
    }

    @Test
    fun testTrx() {
        val user = Namespace("user")
        val _id = ScalarAttr(user["val"], QString)
        val _list = ListAttr(user["list"], QString)

        val conn = qbit(MemStorage())
        conn.persist(_id, _list)

        val e = Entity(_id eq "1", _list eq listOf("1", "2"))
        val se = conn.persist(e).storedEntity()
        assertEquals("1", se[_id])

        val trx = conn.trx()
        trx.persist(se.set(_id eq "2"))

        assertEquals("1", conn.db.pull(se.eid)!![_id])

        trx.rollback()

        assertEquals("1", conn.db.pull(se.eid)!![_id])

        trx.persist(se.set(_id eq "3"))
        trx.commit()

        assertEquals("3", conn.db.pull(se.eid)!![_id])
    }
}


