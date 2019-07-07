package qbit

import qbit.model.*
import qbit.ns.root
import qbit.platform.currentTimeMillis
import qbit.serialization.SimpleSerialization
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class IndexTest {

    private val _uid = ScalarAttr(root["uid"], QInt)
    private val _foo = ScalarAttr(root["foo"], QString)

    @Test
    fun testVaet() {
        val f1 = Fact(EID(1, 0), "attr1", "value1")
        val f2 = Fact(EID(2, 0), "attr2", "value2")
        val f3 = Fact(EID(3, 0), "attr3", "value3")

        assertEquals(0, attrValuePattern("attr2", "value2").invoke(f2))
        assertFalse(attrValuePattern("attr2", "value2").invoke(f1) == 0)

        val byAttr = attrPattern("attr2")
        assertEquals(-1, byAttr(f1))
        assertEquals(0, byAttr(f2))
        assertEquals(1, byAttr(f3))

        val byValue = valuePattern("value2")
        assertEquals(-1, byValue(f1))
        assertEquals(0, byValue(f2))
        assertEquals(1, byValue(f3))

        val byEid = eidPattern(EID(2, 0))
        assertEquals(-1, byEid(f1))
        assertEquals(0, byEid(f2))
        assertEquals(1, byEid(f3))
    }

    @Test
    fun testEntitiesByAttrVal() {
        val idx = Index()
                .addFacts(listOf(f(0, _uid, 0),
                        f(1, _uid, 1),
                        f(0, _uid, 1),
                        f(0, _foo, "baz"),
                        f(1, _foo, "bar"),
                        f(2, _foo, "bar")
                ))

        var lst = idx.eidsByPred(AttrValuePred("/uid", 1))
        assertEquals(2, lst.count())
        assertEquals(0, lst.sorted().toList()[0].eid)
        assertEquals(1, lst.sorted().toList()[1].eid)

        lst = idx.eidsByPred(AttrValuePred("/foo", "bar"))
        assertEquals(2, lst.count())
        assertEquals(1, lst.sorted().toList()[0].eid)
        assertEquals(2, lst.sorted().toList()[1].eid)

        val bazEntities = idx.eidsByPred(attrIs(_foo, "baz")).toList()

        assertEquals(1, bazEntities.size)
        assertEquals(0, bazEntities[0].eid)
    }

    @Test
    fun testEntitiesByAttr() {
        val idx = Index()
                .addFacts(listOf(f(0, _uid, 0),
                        f(1, _uid, 1),
                        f(0, _foo, "bar"),
                        f(1, _foo, "bar"),
                        f(2, _foo, "baz")
                ))

        assertEquals(2, idx.eidsByPred(AttrPred("/uid")).count())
        assertEquals(3, idx.eidsByPred(AttrPred("/foo")).count())
    }

    @Test
    fun testCreateIndex() {
        val dbUuid = DbUuid(IID(0, 1))
        val time1 = currentTimeMillis()
        val eid = EID(0, 0)

        val _attr1 = ScalarAttr(root["attr1"], QInt)
        val _attr2 = ScalarAttr(root["attr2"], QInt)
        val _attr3 = ScalarAttr(root["attr3"], QInt)

        val n1 = Root(null, dbUuid, time1, NodeData(arrayOf(Fact(eid, _attr1, 0))))
        val n2 = Leaf(nullHash, toHashed(n1), dbUuid, time1 + 1, NodeData(arrayOf(
                Fact(eid, _attr1, 1),
                Fact(eid, _attr2, 0))))
        val n3 = Leaf(nullHash, toHashed(n2), dbUuid, time1 + 2, NodeData(arrayOf(
                Fact(eid, _attr1, 2),
                Fact(eid, _attr2, 1),
                Fact(eid, _attr3, 0))))

        val index = Index(Graph { null }, n3)
        assertEquals(0, index.eidsByPred(AttrValuePred("/attr1", 0)).count())
        assertEquals(0, index.eidsByPred(AttrValuePred("/attr1", 1)).count())
        assertEquals(0, index.eidsByPred(AttrValuePred("/attr2", 0)).count())
        assertEquals(1, index.entities.size)
        assertEquals(2, index.entityById(eid)!!.getValue("/attr1")[0])
        assertEquals(1, index.entityById(eid)!!.getValue("/attr2")[0])
        assertEquals(0, index.entityById(eid)!!.getValue("/attr3")[0])
    }

    @Test
    fun testRangeSearch() {
        val dbUuid = DbUuid(IID(0, 1))
        val time1 = currentTimeMillis()
        val eid0 = EID(0, 0)
        val eid1 = EID(0, 1)
        val eid2 = EID(0, 2)
        val eid3 = EID(0, 3)

        val eids = generateSequence(eid0) { eid -> eid.next(1) }
                .iterator()

        val _date = ScalarAttr(root["date"], QLong)

        val e1 = Entity(_date eq 1L)
        val e2 = Entity(_date eq 2L)
        val e3 = Entity(_date eq 3L)
        val e4 = Entity(_date eq 4L)
        val root = Root(Hash(ByteArray(20)), dbUuid, time1, NodeData((e1.toFacts(eids.next()) + e2.toFacts(eids.next()) + e3.toFacts(eids.next()) + e4.toFacts(eids.next())).toTypedArray()))
        val index = Index(Graph { null }, root)

        val vRes = index.eidsByPred(attrIs(_date, 2L))
        assertEquals(1, vRes.count())
        assertEquals(eid1, vRes.first())

        assertArrayEquals(arrayOf(eid0, eid1, eid2),
                index.eidsByPred(attrIn(_date, 1L, 3L)).toList().toTypedArray())
        assertArrayEquals(arrayOf(eid0, eid1, eid2, eid3),
                index.eidsByPred(attrIn(_date, 0L, 5L)).toList().toTypedArray())
        assertArrayEquals(arrayOf(eid1, eid2),
                index.eidsByPred(attrIn(_date, 2L, 3L)).toList().toTypedArray())
        assertArrayEquals(arrayOf(eid0, eid1),
                index.eidsByPred(attrIn(_date, 1L, 2L)).toList().toTypedArray())
        assertArrayEquals(arrayOf(eid1, eid2),
                index.eidsByPred(attrIn(_date, 2L, 3L)).toList().toTypedArray())
    }

    @Test
    fun testLoadTombstones() {
        val dbUuid = DbUuid(IID(0, 1))
        val time1 = currentTimeMillis()
        val eid = EID(0, 0)
        val _attr1 = ScalarAttr(root["attr1"], QInt)

        val n1 = Root(null, dbUuid, time1, NodeData(arrayOf(Fact(eid, _attr1, 0))))
        val n2 = Leaf(nullHash, toHashed(n1), dbUuid, time1 + 1, NodeData(arrayOf(
                Fact(eid, tombstone, true)
        )))
        val index = Index(Graph { null }, n2)
        assertNull(index.entityById(eid))
    }

    private fun toHashed(n: NodeVal<Hash?>): Node<Hash> {
        val data = SimpleSerialization.serializeNode(n)
        val hash = hash(data)
        return when (n) {
            is Root -> Root(hash, n.source, n.timestamp, n.data)
            is Leaf -> Leaf(hash, n.parent, n.source, n.timestamp, n.data)
            else -> throw IllegalArgumentException("Unexpected $n")
        }
    }

    private fun <T : Any> f(eid: Int, attr: Attr<T>, value: T) = Fact(EID(0, eid), attr, value)

}