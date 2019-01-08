package qbit

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import qbit.model.*
import qbit.ns.root
import qbit.model.DataType
import qbit.model.ScalarAttr
import qbit.model._name
import qbit.model._type
import qbit.storage_model.DbUuid
import qbit.storage_model.Graph
import qbit.storage_model.NodeData
import qbit.storage_model.Root
import qbit.util.Hash

class DbTest {

    @Test
    fun testSearchByAttrRangeAndAttrValue() {
        val dbUuid = DbUuid(IID(0, 1))
        val time1 = System.currentTimeMillis()
        val eid2 = EID(0, 4)
        val eids = EID(0, 0).nextEids()

        val _date = ScalarAttr(root["date"], DataType.QLong)
        val _cat = ScalarAttr(root["cat"], DataType.QString)

        val date = Entity(_name eq _date.str(), _type eq DataType.QLong.code)
        val cat = Entity(_name eq _cat.str(), _type eq DataType.QString.code)
        val e1 = Entity(_date eq 1L, _cat eq "C1")
        val e2 = Entity(_date eq 2L, _cat eq "C1")
        val e3 = Entity(_date eq 3L, _cat eq "C2")
        val e4 = Entity(_date eq 4L, _cat eq "C2")
        val root = Root(Hash(ByteArray(20)), dbUuid, time1, NodeData((date.toFacts(eids.next()) + cat.toFacts(eids.next()) + e1.toFacts(eids.next()) + e2.toFacts(eids.next()) + e3.toFacts(eids.next()) + e4.toFacts(eids.next())).toTypedArray()))
        val index = Index(Graph { null }, root)

        val db = IndexDb(index)
        assertArrayEquals(arrayOf(eid2), db.query(attrIn(_date, 1L, 3L), attrIs(_cat, "C2")).map { it.eid }.toTypedArray())
    }

}