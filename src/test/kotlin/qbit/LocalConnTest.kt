package qbit

import org.junit.Assert.*
import org.junit.Test
import qbit.schema.Attr
import qbit.storage.MemStorage

class LocalConnTest {

    @Test
    fun testInit() {
        val db = qbit(MemStorage())
        assertNotNull(db)
    }

    @Test
    fun testUpdate() {
        val conn = qbit(MemStorage())
        val _attr = Attr(Namespace("user")["attr"], QString)
        conn.create(_attr)
        val e = mapOf(_attr to "value")
        val (_, eid) = conn.create(e as Map<Attr<*>, Any>)
        conn.addEntity(eid, mapOf(_attr to "value2"))
        val pulledE2 = conn.db.pull(eid)
        assertEquals("value2", pulledE2!![_attr] as String)
    }

}

