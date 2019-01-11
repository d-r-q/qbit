package qbit

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import qbit.ns.root
import qbit.schema.ScalarAttr
import qbit.schema.eq

class DbTest {

    @Test
    fun testSearchByAttrRangeAndAttrValue() {
        val dbUuid = DbUuid(IID(0, 1))
        val time1 = System.currentTimeMillis()
        val eid2 = EID(0, 4)
        val eids = EID(0, 0).nextEids()

        val _date = ScalarAttr(root["date"], QLong)
        val _cat = ScalarAttr(root["cat"], QString)

        val date = Entity(qbit.EAttr.name eq _date.str(), qbit.EAttr.type eq QLong.code)
        val cat = Entity(qbit.EAttr.name eq _cat.str(), qbit.EAttr.type eq QString.code)
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