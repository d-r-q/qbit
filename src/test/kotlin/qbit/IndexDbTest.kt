package qbit

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import qbit.schema.Attr

class DbTest {

    @Test
    fun testSearchByAttrRangeAndAttrValue() {
        val dbUuid = DbUuid(IID(0, 1))
        val time1 = System.currentTimeMillis()
        val eid0 = EID(0, 2)
        val eid1 = EID(0, 3)
        val eid2 = EID(0, 4)
        val eid3 = EID(0, 5)

        val eids = generateSequence(EID(0, 0)) { eid -> eid.next(1) }
                .iterator()

        val _date = Attr(root["date"], QLong)
        val _cat = Attr(root["cat"], QString)

        val date = Entity(qbit.schema._name to _date.str, qbit.schema._type to QLong.code)
        val cat = Entity(qbit.schema._name to _cat.str, qbit.schema._type to QString.code)
        val e1 = Entity(_date to 1L, _cat to "C1")
        val e2 = Entity(_date to 2L, _cat to "C1")
        val e3 = Entity(_date to 3L, _cat to "C2")
        val e4 = Entity(_date to 4L, _cat to "C2")
        val root = Root(Hash(ByteArray(20)), dbUuid, time1, NodeData((date.toFacts(eids.next()) + cat.toFacts(eids.next()) + e1.toFacts(eids.next()) + e2.toFacts(eids.next()) + e3.toFacts(eids.next()) + e4.toFacts(eids.next())).toTypedArray()))
        val index = Index(Graph { _ -> null }, root)

        val db = IndexDb(index)
        assertArrayEquals(arrayOf(eid2), db.query(attrIn(_date, 1L, 3L), attrIs(_cat, "C2")).map { it.eid }.toTypedArray())
    }
}