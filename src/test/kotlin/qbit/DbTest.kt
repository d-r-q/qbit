package qbit

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class DbTest {

    @Test
    fun testInit() {
        val db = Db()
        assertEquals(VersionVector(0, listOf(1)), db.version())
    }

    @Test
    fun testUpdate() {
        val db = Db()
        val e = mapOf("attr" to "value")
        val eid = db.create(e)
        db.add(eid, "attr" to "value2")
        val pulledE2 = db.pull(eid)
        assertEquals("value2", pulledE2!!["attr"] as String)
    }

    @Test
    fun testSync() {
        val db1 = Db()
        val db2Uuid = UUID.randomUUID().toString()
        db1.addDb(db2Uuid)
        val db2 = Db(db2Uuid, db1)

        val e1 = mapOf("attr1" to "value1")
        val e1id = db1.create(e1)

        val e2 = mapOf("attr2" to "value2")
        val e2id = db2.create(e2)

        db2.sync(db1)
        assertEquals("value1", db1.pull(e1id)!!["attr1"])
        assertEquals("value2", db1.pull(e2id)!!["attr2"])
        assertEquals("value1", db2.pull(e1id)!!["attr1"])
        assertEquals("value2", db2.pull(e2id)!!["attr2"])
    }

}

