package qbit

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import qbit.model.*
import qbit.ns.root

class DbTest {

    @Test
    fun testSearchByAttrRangeAndAttrValue() {
        val dbUuid = DbUuid(IID(0, 1))
        val time1 = System.currentTimeMillis()
        val eid2 = EID(0, 4)
        val eids = EID(0, 0).nextEids()

        val _date = ScalarAttr(root["date"], QLong)
        val _cat = ScalarAttr(root["cat"], QString)

        val date = Entity(EAttr.name eq _date.str(), EAttr.type eq QLong.code)
        val cat = Entity(EAttr.name eq _cat.str(), EAttr.type eq QString.code)
        val e1 = Entity(_date eq 1L, _cat eq "C1")
        val e2 = Entity(_date eq 2L, _cat eq "C1")
        val e3 = Entity(_date eq 3L, _cat eq "C2")
        val e4 = Entity(_date eq 4L, _cat eq "C2")
        val root = Root(Hash(ByteArray(20)), dbUuid, time1, NodeData((date.toFacts(eids.next()) + cat.toFacts(eids.next()) + e1.toFacts(eids.next()) + e2.toFacts(eids.next()) + e3.toFacts(eids.next()) + e4.toFacts(eids.next())).toTypedArray()))
        val index = Index(Graph { null }, root)

        val db = IndexDb(index, root.hash)
        assertArrayEquals(arrayOf(eid2), db.query(attrIn(_date, 1L, 3L), attrIs(_cat, "C2")).map { it.eid }.toList().toTypedArray())
    }

    @Test
    fun testFetchEidsDeduplication() {
        val dbUuid = DbUuid(IID(0, 1))
        val time1 = System.currentTimeMillis()
        val eids = EID(0, 0).nextEids()

        val _cat = ListAttr(root["cat"], QString)

        val e1 = Entity(_cat eq listOf("C1", "C2"))
        val theEid = eids.next()
        val root = Root(Hash(ByteArray(20)), dbUuid, time1, NodeData((_cat.toFacts(eids.next()) + e1.toFacts(theEid)).toTypedArray()))
        val index = Index(Graph { null }, root)

        val db = IndexDb(index, root.hash)
        assertArrayEquals(arrayOf(theEid), db.query(attrIn(_cat, "C1", "C3")).map { it.eid }.toList().toTypedArray())
    }

    @Test
    fun testIndexMultipleTransactions() {
        val dbUuid = DbUuid(IID(0, 1))
        val attr = ScalarAttr(root["aattr"], QString)
        val eids = EID(0, 0).nextEids()

        val e1 = Entity(attr eq "avalue1")
        val eid1 = eids.next()
        val root = Root(Hash(byteArrayOf(0)), dbUuid, System.currentTimeMillis(), NodeData((attr.toFacts(eids.next()) + e1.toFacts(eid1)).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.hash to root)
        val graph = Graph { nodes[it.hash] }
        var indexDb = IndexDb(Index(graph, root), root.hash)

        val e2 = Entity(attr eq "avalue2")
        val eid2 = eids.next()
        val n1 = Leaf(Hash(byteArrayOf(1)), root, dbUuid, System.currentTimeMillis(), NodeData((e2.toFacts(eid2)).toTypedArray()))
        nodes[n1.hash] = n1

        val e3 = Entity(attr eq "avalue3")
        val eid3 = eids.next()
        val n2 = Leaf(Hash(byteArrayOf(2)), n1, dbUuid, System.currentTimeMillis(), NodeData((e3.toFacts(eid3)).toTypedArray()))
        nodes[n2.hash] = n2

        indexDb = db(graph, indexDb, n2)
        assertNotNull(indexDb.pull(eid1))
        assertNotNull(indexDb.pull(eid2))
        assertNotNull(indexDb.pull(eid3))
    }

    @Test
    fun testUpdateInTrx() {
        val dbUuid = DbUuid(IID(0, 1))
        val attr = ScalarAttr(root["aattr"], QString)
        val eids = EID(0, 0).nextEids()

        val e1 = Entity(attr eq "avalue1")
        val eid1 = eids.next()
        val root = Root(Hash(byteArrayOf(0)), dbUuid, System.currentTimeMillis(), NodeData((attr.toFacts(eids.next()) + e1.toFacts(eid1)).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.hash to root)
        val graph = Graph { nodes[it.hash] }
        var indexDb = IndexDb(Index(graph, root), root.hash)

        val e2 = Entity(attr eq "avalue2")
        val eid2 = eids.next()
        val n1 = Leaf(Hash(byteArrayOf(1)), root, dbUuid, System.currentTimeMillis(), NodeData((e2.toFacts(eid2)).toTypedArray()))
        nodes[n1.hash] = n1

        val e3 = Entity(attr eq "avalue2.1")
        val n2 = Leaf(Hash(byteArrayOf(2)), n1, dbUuid, System.currentTimeMillis(), NodeData((e3.toFacts(eid2)).toTypedArray()))
        nodes[n2.hash] = n2

        indexDb = db(graph, indexDb, n2)
        assertNotNull(indexDb.pull(eid1))
        assertNotNull(indexDb.pull(eid2))
        assertNotNull(indexDb.query(attrIs(attr, "avalue2.1")).firstOrNull())
    }
}