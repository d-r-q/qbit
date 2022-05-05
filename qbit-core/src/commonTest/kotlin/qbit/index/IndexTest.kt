package qbit.index

import qbit.Attr
import qbit.Entity
import qbit.Scientists.extId
import qbit.TestIndexer
import qbit.api.db.AttrPred
import qbit.api.db.AttrValuePred
import qbit.api.db.attrIn
import qbit.api.db.attrIs
import qbit.api.gid.Gid
import qbit.api.gid.Iid
import qbit.api.model.*
import qbit.api.system.DbUuid
import qbit.assertArrayEquals
import qbit.platform.currentTimeMillis
import qbit.platform.runBlocking
import qbit.serialization.*
import qbit.trx.toFacts
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import qbit.Scientists.name as userName

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
            .addFacts(
                listOf(
                    f(0, extId, 0),
                    f(1, extId, 1),
                    f(0, extId, 1),
                    f(0, userName, "baz"),
                    f(1, userName, "bar"),
                    f(2, userName, "bar")
                ),
                null,
                emptyList()
            )

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
            .addFacts(
                listOf(
                    f(0, extId, 0),
                    f(1, extId, 1),
                    f(0, userName, "bar"),
                    f(1, userName, "bar"),
                    f(2, userName, "baz")
                ),
                null,
                emptyList()
            )

        assertEquals(2, idx.eidsByPred(AttrPred(extId.name)).count())
        assertEquals(3, idx.eidsByPred(AttrPred(userName.name)).count())
    }

    @Test
    fun testCreateIndex() {
        runBlocking {
            val dbUuid = DbUuid(Iid(0, 1))
            val time1 = currentTimeMillis()
            val eid = Gid(0, 0)

            val _attr1 = "/attr1"
            val _attr2 = "/attr2"
            val _attr3 = "/attr3"

            val n1 = Root(null, dbUuid, time1, NodeData(arrayOf(Eav(eid, _attr1, 0))))
            val n2 = Leaf(
                nullHash, toHashed(n1), dbUuid, time1 + 1,
                NodeData(
                    arrayOf(
                        Eav(eid, _attr1, 1),
                        Eav(eid, _attr2, 0)
                    )
                )
            )
            val n3 = Leaf(
                nullHash, toHashed(n2), dbUuid, time1 + 2,
                NodeData(
                    arrayOf(
                        Eav(eid, _attr1, 2),
                        Eav(eid, _attr2, 1),
                        Eav(eid, _attr3, 0)
                    )
                )
            )

            val index = TestIndexer().index(n3).index
            assertEquals(0, index.eidsByPred(AttrValuePred("/attr1", 0)).count())
            assertEquals(0, index.eidsByPred(AttrValuePred("/attr1", 1)).count())
            assertEquals(0, index.eidsByPred(AttrValuePred("/attr2", 0)).count())
            assertEquals(1, index.entities.size)
            assertEquals(2, index.entityById(eid)!!.getValue("/attr1")[0])
            assertEquals(1, index.entityById(eid)!!.getValue("/attr2")[0])
            assertEquals(0, index.entityById(eid)!!.getValue("/attr3")[0])
        }
    }

    @Test
    fun testRangeSearch() {
        runBlocking {
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
            val root = Root(
                Hash(ByteArray(20)),
                dbUuid,
                time1,
                NodeData((e1.toFacts() + e2.toFacts() + e3.toFacts() + e4.toFacts()).toTypedArray())
            )
            val index = TestIndexer().index(root).index

            val vRes = index.eidsByPred(attrIs(_date, 2L))
            assertEquals(1, vRes.count())
            assertEquals(eid1, vRes.first())

            assertArrayEquals(
                arrayOf(eid0, eid1, eid2),
                index.eidsByPred(attrIn(_date, 1L, 3L)).toList().toTypedArray()
            )
            assertArrayEquals(
                arrayOf(eid0, eid1, eid2, eid3),
                index.eidsByPred(attrIn(_date, 0L, 5L)).toList().toTypedArray()
            )
            assertArrayEquals(
                arrayOf(eid1, eid2),
                index.eidsByPred(attrIn(_date, 2L, 3L)).toList().toTypedArray()
            )
            assertArrayEquals(
                arrayOf(eid0, eid1),
                index.eidsByPred(attrIn(_date, 1L, 2L)).toList().toTypedArray()
            )
            assertArrayEquals(
                arrayOf(eid1, eid2),
                index.eidsByPred(attrIn(_date, 2L, 3L)).toList().toTypedArray()
            )
        }
    }

/*
    @Test
    fun testLoadTombstones() {
        val dbUuid = DbUuid(Iid(0, 1))
        val time1 = currentTimeMillis()
        val eid = Gid(0, 0)
        val _attr1 = Attr<Int>("attr1")

        val n1 = Root(null, dbUuid, time1, NodeData(arrayOf(Eav(eid, _attr1, 0))))
        val n2 = Leaf(nullHash, toHashed(n1), dbUuid, time1 + 1, NodeData(arrayOf(
                Eav(eid, tsAttr, true)
        )))
        val index = TestIndexer().index(n2).index
        assertNull(index.entityById(eid))
    }
*/

    @JsName("Test_putting_tombstone_into_index_should_filter_all_facts_of_corresponding_entity")
    @Test
    fun `Test putting tombstone into index should filter all facts of corresponding entity`() {
        val deletedEntityGid = Gid(0, 0)
        val idx = Index(
            listOf(
                deletedEntityGid to listOf(Eav(deletedEntityGid, "any", "any")),
                Gid(0, 1) to listOf(Eav(Gid(0, 1), "to-keep", "any"))
            )
        )
        val filtered = idx.addFacts(listOf(Eav(deletedEntityGid, qbit.api.tombstone.name, true)), null, emptyList())
        assertEquals(1, filtered.entities.size)
        assertEquals(1, filtered.aveIndex.size)
        assertNotNull(filtered.eidsByPred(AttrValuePred("to-keep", "any")).firstOrNull(), "Cannot find entity by to-keep=any")
    }

    @JsName("Test_putting_tombstone_into_index_should_remove_all_facts_of_corresponding_entity_even_if_facts_of_origin_entity_are_unsorted")
    @Test
    fun `Test putting tombstone into index should remove all facts of corresponding entity even if facts of origin entity are unsorted`() {
        // Given an index with two entities with interleaving eavs
        val deletedGid = Gid(0, 0)
        val anotherGid = Gid(0, 1)
        val deletedEavs = listOf(
            Eav(deletedGid, "attr3", "c"),
            Eav(deletedGid, "attr2", "b"),
            Eav(deletedGid, "attr1", "a")
        )
        val anotherEavs = listOf(
            Eav(anotherGid, "attr1", "b"),
            Eav(anotherGid, "attr2", "a")
        )
        val index = Index(listOf(deletedGid to deletedEavs, anotherGid to anotherEavs))

        // When to the index add tombstone for deleted entity
        val filtered = index.addFacts((listOf(Eav(deletedGid, qbit.api.tombstone.name, true))))

        // Then should contain only another entity and it's eavs
        assertEquals(1, filtered.entities.size)
        assertEquals(2, filtered.aveIndex.size)
        assertNotNull(filtered.entityById(anotherGid))
        assertNotNull(filtered.eidsByPred(AttrValuePred("attr2", "a")).firstOrNull(), "Cannot find another entity by attr2=a")
        assertNotNull(filtered.eidsByPred(AttrValuePred("attr1", "b")).firstOrNull(), "Cannot find another entity by attr1=b")
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