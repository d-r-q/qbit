package qbit

import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.schema.Attr

class IndexTest {

    private val _uid = Attr(root["uid"], QInt)
    private val _foo = Attr(root["foo"], QString)

    @Test
    fun testVaet() {
        val f1 = Fact(EID(1, 0), "attr1", "value1")
        val f2 = Fact(EID(2, 0), "attr2", "value2")
        val f3 = Fact(EID(3, 0), "attr3", "value3")

        assertEquals(0, attrValuePattern("attr2", "value2").invoke(f2))

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
                .add(listOf(f(0, _uid, 0),
                        f(1, _uid, 1),
                        f(0, _uid, 1),
                        f(0, _foo, "bar"),
                        f(1, _foo, "bar"),
                        f(2, _foo, "baz")
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
                .add(listOf(f(0, _uid, 0),
                        f(0, _uid, 1)))
        assertEquals(0, idx.entitiesByAttrVal("/uid", 0).size)
        assertEquals(1, idx.entitiesByAttrVal("/uid", 1).size)

        val idx2 = idx.add(f(0, _uid, 2))
        assertEquals(0, idx2.entitiesByAttrVal("/uid", 0).size)
        assertEquals(0, idx2.entitiesByAttrVal("/uid", 1).size)
        assertEquals(1, idx2.entitiesByAttrVal("/uid", 2).size)
    }

    @Test
    fun testEntitiesByAttr() {
        val idx = Index()
                .add(listOf(f(0, _uid, 0),
                        f(1, _uid, 1),
                        f(0, _foo, "bar"),
                        f(1, _foo, "bar"),
                        f(2, _foo, "baz")
                ))

        assertEquals(2, idx.entitiesByAttr("/uid").size)
        assertEquals(3, idx.entitiesByAttr("/foo").size)
    }

    private fun <T : Any> f(eid: Int, attr: Attr<T>, value: T) = Fact(EID(0, eid), attr, value)
}