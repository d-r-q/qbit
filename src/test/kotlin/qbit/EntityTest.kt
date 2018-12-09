package qbit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import qbit.ns.Namespace
import qbit.ns.root
import qbit.schema.RefAttr
import qbit.schema.ScalarAttr

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

}