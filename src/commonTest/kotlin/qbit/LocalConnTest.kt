package qbit

import qbit.model.*
import qbit.ns.Namespace
import qbit.ns.root
import qbit.platform.*
import qbit.storage.MemStorage
import kotlin.test.*

class LocalConnTest {

    @Test
    fun testInit() {
        val db = qbit(MemStorage())
        assertNotNull(db)
        assertTrue(db.storage.keys(Namespace("nodes")).isNotEmpty())
    }

    @Test
    fun testUpdate() {
        val conn = qbit(MemStorage())
        val _attr = ScalarAttr(Namespace("user")["attr"], QString)
        conn.persist(_attr)
        val e = Entity(_attr eq "value")
        var se = conn.persist(e).storedEntity()
        se = se.with(_attr, "value2")
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
        se = se.with(_attr, "value2")
        conn.persist(se)

        val pulledE2 = conn.db.pull(se.eid)
        assertEquals("value2", pulledE2!![_attr])

        conn.persist(pulledE2.tombstone())

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
        e1 = e1.with(_ref, e3)

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
        val se2 = conn.persist(se1.with(notUnique, "notUnique2")).storedEntity()
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
        conn.persist(se1.with(_val, "e1"))
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
        se1 = se1.with(_val, "e1.1")
        se2 = se2.with(_val, "e2.1")

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

        se = conn.persist(se.with(_list, emptyList())).storedEntity()
        assertNull(se.tryGet(_list))
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
        trx.persist(se.with(_id eq "2"))

        assertEquals("1", conn.db.pull(se.eid)!![_id])

        trx.rollback()

        assertEquals("1", conn.db.pull(se.eid)!![_id])

        trx.persist(se.with(_id eq "3"))
        trx.commit()

        assertEquals("3", conn.db.pull(se.eid)!![_id])
    }

    @Test
    fun testPersistDataTypes() {
        val bool = ScalarAttr(root["bool"], QBoolean)
        val boolList = ListAttr(root["boolList"], QBoolean)
        val byt = ScalarAttr(root["byte"], QByte)
        val bytList = ListAttr(root["byteList"], QByte)
        val nt = ScalarAttr(root["int"], QInt)
        val ntList = ListAttr(root["intList"], QInt)
        val lng = ScalarAttr(root["long"], QLong)
        val lngList = ListAttr(root["longList"], QLong)
        val str = ScalarAttr(root["str"], QString)
        val strList = ListAttr(root["strList"], QString)
        val bytes = ScalarAttr(root["bytes"], QBytes)
        val bytesList = ListAttr(root["bytesList"], QBytes)
        val eid = ScalarAttr(root["eid"], QEID)
        val eidList = ListAttr(root["eidList"], QEID)
        val instant = ScalarAttr(root["instant"], QInstant)
        val instantList = ListAttr(root["instantList"], QInstant)
        val dt = ScalarAttr(root["dateTime"], QZonedDateTime)
        val dtList = ListAttr(root["dateTimeList"], QZonedDateTime)
        val decimal = ScalarAttr(root["decimal"], QDecimal)
        val decimalList = ListAttr(root["decimalList"], QDecimal)

        val boolVal = true
        val boolListVal = listOf(true, false)
        val byteVal = 0.toByte()
        val byteListVal = listOf(Byte.MIN_VALUE, Byte.MAX_VALUE)
        val ntVal = 0
        val ntListVal = listOf(Int.MIN_VALUE, Int.MAX_VALUE)
        val lngVal = 0L
        val lngListVal = listOf(Long.MIN_VALUE, Long.MAX_VALUE)
        val strVal = ""
        val strListVal = listOf("String", "Строка", "ライン", "线", "שורה")
        val bytesVal = byteArrayOf()
        val bytesListVal = listOf(byteArrayOf(1), byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE))
        val eidVal = EID(0)
        val eidListVal = listOf(EID(Long.MIN_VALUE), EID(Long.MAX_VALUE))
        val instantVal = Instants.now()
        val instantListVal = listOf(Instants.ofEpochMilli(Int.MIN_VALUE.toLong()), Instants.ofEpochMilli(0), Instants.ofEpochMilli(Int.MAX_VALUE.toLong()))
        val dtVal = ZonedDateTimes.now()
        val dtListVal = listOf(ZonedDateTimes.of(-2200, 1, 1, 0, 0, 0, 0, ZoneIds.of("+01:00")),
                ZonedDateTimes.now(ZoneIds.of("Europe/Moscow")),
                ZonedDateTimes.of(2200, 12, 31, 23, 59, 59, 999999999, ZoneIds.of("Z")))
        val decimalVal = BigDecimal(0)
        val decimalListVal = listOf(BigDecimal(Long.MIN_VALUE).minus(BigDecimal(1)), BigDecimal(Long.MAX_VALUE).plus(BigDecimal(1)))

        val entity = Entity(bool eq boolVal, boolList eq boolListVal, byt eq byteVal, bytList eq byteListVal,
                nt eq ntVal, ntList eq ntListVal, lng eq lngVal, lngList eq lngListVal, str eq strVal, strList eq strListVal,
                bytes eq bytesVal, bytesList eq bytesListVal, eid eq eidVal, eidList eq eidListVal, instant eq instantVal, instantList eq instantListVal,
                dt eq dtVal, dtList eq dtListVal, decimal eq decimalVal, decimalList eq decimalListVal)
        val conn = qbit(MemStorage())
        val eeid = conn.persist(entity, bool, boolList, byt, bytList, nt, ntList, lng, lngList, str, strList,
                bytes, bytesList, eid, eidList, instant, instantList, dt, dtList, decimal, decimalList).storedEntity().eid
        val stored = conn.db.pull(eeid)!!

        assertEquals(boolVal, stored[bool])
        assertEquals(boolListVal, stored[boolList])
        assertEquals(byteVal, stored[byt])
        assertEquals(byteListVal, stored[bytList])
        assertEquals(ntVal, stored[nt])
        assertEquals(ntListVal, stored[ntList])
        assertEquals(lngVal, stored[lng])
        assertEquals(lngListVal, stored[lngList])
        assertEquals(strVal, stored[str])
        assertEquals(strListVal, stored[strList])
        assertEquals(bytesVal, stored[bytes])
        assertEquals(bytesListVal, stored[bytesList])
        assertEquals(eidVal, stored[eid])
        assertEquals(eidListVal, stored[eidList])
        assertEquals(instantVal, stored[instant])
        assertEquals(instantListVal, stored[instantList])
        assertEquals(dtVal, stored[dt])
        assertEquals(dtListVal, stored[dtList])
        assertEquals(decimalVal, stored[decimal])
        assertEquals(decimalListVal, stored[decimalList])
    }
}


