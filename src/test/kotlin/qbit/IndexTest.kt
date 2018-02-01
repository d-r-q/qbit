package qbit

import org.junit.Assert.assertEquals
import org.junit.Test

class IndexTest {

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
    fun testEntitiesByAttr() {
        val idx = Index()
                .add(listOf(f(0, "uid", 0, 0),
                        f(0, "uid", 1, 1),
                        f(1, "uid", 0, 1),
                        f(0, "foo", 0, "bar"),
                        f(1, "foo", 0, "bar"),
                        f(2, "foo", 0, "baz")
                ))

        var lst = idx.entitiesByAttr("uid", 0)
        assertEquals(1, lst.size)
        assertEquals(0, lst.first().eid)

        lst = idx.entitiesByAttr("uid", 1)
        assertEquals(2, lst.size)
        assertEquals(0, lst.sorted().toList()[0].eid)
        assertEquals(1, lst.sorted().toList()[1].eid)

        lst = idx.entitiesByAttr("foo", "bar")
        assertEquals(2, lst.size)
        assertEquals(0, lst.sorted().toList()[0].eid)
        assertEquals(1, lst.sorted().toList()[1].eid)
    }

    private fun f(eid: Int, attr: String, time: Long, value: Any) = StoredFact(EID(0, eid), attr, time, value)
}