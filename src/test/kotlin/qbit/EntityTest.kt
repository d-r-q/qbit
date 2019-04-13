package qbit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import qbit.ns.Namespace
import qbit.ns.root
import qbit.schema.ListAttr
import qbit.schema.RefAttr
import qbit.schema.ScalarAttr
import qbit.schema.eq

class EntityTest {

    @Test
    fun testCreate() {
        val user = Namespace("user")
        val _attr = ScalarAttr(user["attr"], QString)
        val _ref = RefAttr(user["ref"])
        val _eid = ScalarAttr(user["some_eid"], QEID)

        val e1 = Entity(_attr eq "e1")
        assertTrue((e1 as MapEntity).refs.isEmpty())
        val e2 = Entity(_attr eq "e2", _ref eq e1, _eid eq EID(0, 3))
        assertTrue((e2 as MapEntity).map.size == 2)
        assertTrue(e2.refs.size == 1)

        assertEquals("e2", e2[_attr])
        assertEquals(EID(0, 3), e2[_eid])
        assertTrue(e2[_ref] === e1)
        assertEquals(3, e2.entries.size)
    }

    @Test
    fun testPutRef() {
        var first = Entity()
        val second = Entity()
        val attr = RefAttr(root["test"])
        first = first.set(attr, second)
        assertTrue(second === first[attr])
    }

    @Test
    fun testListAttr() {
        var e = Entity()
        val attr = ListAttr(root["test"], QString)
        e = e.set(attr, listOf("first"))
        assertEquals(listOf("first"), e[attr])
    }

    @Test
    fun testSetAttrs() {
        var e = Entity()
        val _first = ScalarAttr(root["first"], QLong)
        val _second = RefAttr(root["second"])
        val _third = ListAttr(root["third"], QString)
        val referee = Entity()
        e = e.set(_first eq 1,
                _second eq referee,
                _third eq listOf("3"))
        assertEquals(1, e[_first])
        assertEquals(referee, e[_second])
        assertEquals(listOf("3"), e[_third])

    }
}