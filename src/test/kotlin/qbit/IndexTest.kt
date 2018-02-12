package qbit

import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.schema.Attr

class IndexTest {

    private val _uid = Attr(root["uid"], QInt)
    private val _foo = Attr(root["foo"], QString)

    @Test
    fun testVaet() {
        val f1 = StoredFact(EID(1, 0), "attr2", 1, "value2")
        assertEquals(0, avetCmp(f1, f1))

        val byAttr = FactPattern(null, "attr2", null, null)
        assertEquals(1, avetCmp(f1, byAttr))
        assertEquals(-1, avetCmp(byAttr, f1))

        val lesserAttr = FactPattern(null, "attr1", null, null)
        assertEquals(1, avetCmp(f1, lesserAttr))
        assertEquals(-1, avetCmp(lesserAttr, f1))

        val greaterAttr = FactPattern(null, "attr3", null, null)
        assertEquals(-1, avetCmp(f1, greaterAttr))
        assertEquals(1, avetCmp(greaterAttr, f1))

        val lesserValue = FactPattern(null, "attr2", null, "value1")
        assertEquals(1, avetCmp(f1, lesserValue))
        assertEquals(-1, avetCmp(lesserValue, f1))

        val greaterValue = FactPattern(null, "attr2", null, "value3")
        assertEquals(-1, avetCmp(f1, greaterValue))
        assertEquals(1, avetCmp(greaterValue, f1))

        val lesserEid = FactPattern(EID(0, 0), "attr2", null, "value2")
        assertEquals(1, avetCmp(f1, lesserEid))
        assertEquals(-1, avetCmp(lesserEid, f1))

        val greaterEid = FactPattern(EID(2, 0), "attr2", null, "value2")
        assertEquals(-1, avetCmp(f1, greaterEid))
        assertEquals(1, avetCmp(greaterEid, f1))

        val lesserTime = FactPattern(EID(1, 0), "attr2", 0, "value2")
        assertEquals(1, avetCmp(f1, lesserTime))
        assertEquals(-1, avetCmp(lesserTime, f1))

        val greaterTime = FactPattern(EID(1, 0), "attr2", 3, "value2")
        assertEquals(-1, avetCmp(f1, greaterTime))
        assertEquals(1, avetCmp(greaterTime, f1))

    }

    @Test
    fun testEntitiesByAttrVal() {
        val idx = Index()
                .add(listOf(f(0, _uid, 0, 0),
                        f(0, _uid, 1, 1),
                        f(1, _uid, 0, 1),
                        f(0, _foo, 0, "bar"),
                        f(1, _foo, 0, "bar"),
                        f(2, _foo, 0, "baz")
                ))

        var lst = idx.entitiesByAttrVal("/uid", 1)
        assertEquals(2, lst.size)
        assertEquals(0, lst.sorted().toList()[0].eid)
        assertEquals(1, lst.sorted().toList()[1].eid)

        lst = idx.entitiesByAttrVal("/foo", "bar")
        assertEquals(2, lst.size)
        assertEquals(0, lst.sorted().toList()[0].eid)
        assertEquals(1, lst.sorted().toList()[1].eid)
    }

    @Test
    fun testEntityByOldAttrValue() {
        val idx = Index()
                .add(listOf(f(0, _uid,0, 0),
                        f(0, _uid,1, 1)))
        assertEquals(0, idx.entitiesByAttrVal("/uid", 0).size)
        assertEquals(1, idx.entitiesByAttrVal("/uid", 1).size)

        val idx2 = idx.add(f(0, _uid, 2, 2))
        assertEquals(0, idx2.entitiesByAttrVal("/uid", 0).size)
        assertEquals(0, idx2.entitiesByAttrVal("/uid", 1).size)
        assertEquals(1, idx2.entitiesByAttrVal("/uid", 2).size)
    }

    @Test
    fun testEntitiesByAttr() {
        val idx = Index()
                .add(listOf(f(0, _uid, 0, 0),
                        f(1, _uid, 0, 1),
                        f(0, _foo, 0, "bar"),
                        f(1, _foo, 0, "bar"),
                        f(2, _foo, 0, "baz")
                ))

        assertEquals(2, idx.entitiesByAttr("/uid").size)
        assertEquals(3, idx.entitiesByAttr("/foo").size)
    }

    private fun <T : Any> f(eid: Int, attr: Attr<T>, time: Long, value: T) = StoredFact(EID(0, eid), attr.str, time, value)
}