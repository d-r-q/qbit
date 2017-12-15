package qbit

import org.junit.Assert.*
import org.junit.Test
import qbit.storage.MemStorage

class DbTest {

    @Test
    fun testInit() {
        val db = Db(MemStorage())
        assertNotNull(db)
    }

    @Test
    fun testUpdate() {
        val db = Db(MemStorage())
        val e = mapOf("attr" to "value")
        val eid = db.create(e)
        db.add(eid, mapOf("attr" to "value2"))
        val pulledE2 = db.pull(eid)
        assertEquals("value2", pulledE2!!["attr"] as String)
    }

    @Test
    fun testSync() {
        val db1 = Db(MemStorage())
        val (id, head) = db1.fork()
        val db2 = Db(MemStorage(), id, head)

        val e1 = mapOf("attr1" to "value1")
        val e1id = db1.create(e1)

        val e2 = mapOf("attr2" to "value2")
        val e2id = db2.create(e2)

        assertTrue(db2.sync(db1).isOk)
        assertEquals("value1", db1.pull(e1id)!!["attr1"])
        assertEquals("value2", db1.pull(e2id)!!["attr2"])
        assertEquals("value1", db2.pull(e1id)!!["attr1"])
        assertEquals("value2", db2.pull(e2id)!!["attr2"])
    }

}

