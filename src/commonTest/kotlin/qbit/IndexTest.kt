package qbit

import qbit.Scientists.extId
import qbit.system.DbUuid
import qbit.index.Indexer
import qbit.index.*
import qbit.model.*
import qbit.platform.currentTimeMillis
import qbit.query.AttrPred
import qbit.query.AttrValuePred
import qbit.query.attrIn
import qbit.query.attrIs
import qbit.serialization.*
import qbit.util.Hash
import qbit.util.hash
import qbit.util.nullHash
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import qbit.Scientists.name as userName
import qbit.model.tombstone as tsAttr

class IndexTest {

    @Test
    fun testVaet() {
        val f1 = Eav(Gid(1, 0), "attr1", "value1")
        val f2 = Eav(Gid(2, 0), "attr2", "value2")
        val f3 = Eav(Gid(3, 0), "attr3", "value3")

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

        val byEid = eidPattern(Gid(2, 0))
        assertEquals(-1, byEid(f1))
        assertEquals(0, byEid(f2))
        assertEquals(1, byEid(f3))
    }

    @Test
    fun testEntitiesByAttrVal() {
        val idx = Index()
                .addFacts(listOf(f(0, extId, 0),
                        f(1, extId, 1),
                        f(0, extId, 1),
                        f(0, userName, "baz"),
                        f(1, userName, "bar"),
                        f(2, userName, "bar")
                ))

        var lst = idx.eidsByPred(AttrValuePred(extId.name, 1))
        assertEquals(2, lst.count())
        assertEquals(0, lst.sorted().toList()[0].eid)
        assertEquals(1, lst.sorted().toList()[1].eid)

        lst = idx.eidsByPred(AttrValuePred(userName.name, "bar"))
        assertEquals(2, lst.count())
        assertEquals(1, lst.sorted().toList()[0].eid)
        assertEquals(2, lst.sorted().toList()[1].eid)

        val bazEntities = idx.eidsByPred(attrIs(userName, "baz")).toList()

        assertEquals(1, bazEntities.size)
        assertEquals(0, bazEntities[0].eid)
    }

    @Test
    fun testEntitiesByAttr() {
        val idx = Index()
                .addFacts(listOf(f(0, extId, 0),
                        f(1, extId, 1),
                        f(0, userName, "bar"),
                        f(1, userName, "bar"),
                        f(2, userName, "baz")
                ))

        assertEquals(2, idx.eidsByPred(AttrPred(extId.name)).count())
        assertEquals(3, idx.eidsByPred(AttrPred(userName.name)).count())
    }

    @Test
    fun testCreateIndex() {
        val dbUuid = DbUuid(Iid(0, 1))
        val time1 = currentTimeMillis()
        val eid = Gid(0, 0)

        val _attr1 = "/attr1"
        val _attr2 = "/attr2"
        val _attr3 = "/attr3"

        val n1 = Root(null, dbUuid, time1, NodeData(arrayOf(Eav(eid, _attr1, 0))))
        val n2 = Leaf(nullHash, toHashed(n1), dbUuid, time1 + 1,
                NodeData(arrayOf(
                        Eav(eid, _attr1, 1),
                        Eav(eid, _attr2, 0))))
        val n3 = Leaf(nullHash, toHashed(n2), dbUuid, time1 + 2,
                NodeData(arrayOf(
                        Eav(eid, _attr1, 2),
                        Eav(eid, _attr2, 1),
                        Eav(eid, _attr3, 0))))

        val index = Indexer(null, null, identityNodeResolver).index(n3).index
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
        val dbUuid = DbUuid(Iid(0, 1))
        val time1 = currentTimeMillis()
        val eid0 = Gid(0, 0)
        val eid1 = Gid(0, 1)
        val eid2 = Gid(0, 2)
        val eid3 = Gid(0, 3)

        val _date = Attr<Long>("date")

        val e1 = Entity(eid0, _date eq 1L)
        val e2 = Entity(eid1, _date eq 2L)
        val e3 = Entity(eid2, _date eq 3L)
        val e4 = Entity(eid3, _date eq 4L)
        val root = Root(Hash(ByteArray(20)), dbUuid, time1, NodeData((e1.toFacts() + e2.toFacts() + e3.toFacts() + e4.toFacts()).toTypedArray()))
        val index = Indexer(null, null, identityNodeResolver).index(root).index

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
        val dbUuid = DbUuid(Iid(0, 1))
        val time1 = currentTimeMillis()
        val eid = Gid(0, 0)
        val _attr1 = Attr<Int>("attr1")

        val n1 = Root(null, dbUuid, time1, NodeData(arrayOf(Fact(eid, _attr1, 0))))
        val n2 = Leaf(nullHash, toHashed(n1), dbUuid, time1 + 1, NodeData(arrayOf(
                Fact(eid, tsAttr, true)
        )))
        val index = Indexer(null, null, identityNodeResolver).index(n2).index
        assertNull(index.entityById(eid))
    }

    @Test
    fun `Test putting tombstone into index should filter all facts of correspondingEntity`() {
        val idx = Index(listOf(Gid(0, 0) to listOf(Eav(Gid(0, 0), "any", "any")),
                Gid(0, 1) to listOf(Eav(Gid(0, 1), "to-keep", "any"))))
        val filtered = idx.addFacts(listOf(Eav(Gid(0, 0), qbit.model.tombstone.name, true)))
        assertEquals(1, filtered.entities.size)
        assertEquals(2, filtered.indices.size)
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

    private fun <T : Any> f(eid: Int, attr: Attr<T>, value: T) = Eav(Gid(0, eid), attr.name, value)

}