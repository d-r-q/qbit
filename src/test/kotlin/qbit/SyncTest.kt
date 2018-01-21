package qbit

import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.storage.FileSystemStorage
import java.nio.file.Files

class SyncTest {

    @Test
    fun testSync() {
        val db1Storage = FileSystemStorage(Files.createTempDirectory("qbit-db1-"))
        val conn1 = qbit(db1Storage)

        val (id, head) = conn1.fork()

        val db2Storage = FileSystemStorage(Files.createTempDirectory("qbit-db2-"))
        val conn2 = LocalConn(id, db2Storage, head)
        conn2.fetch(conn1)

        val e1 = mapOf("attr1" to "value1")
        val (_, e1id) = conn1.create(e1)

        val e2 = mapOf("attr2" to "value2")
        val (_, e2id) = conn2.create(e2)

        conn2.sync(conn1)
        assertEquals("value1", conn1.db.pull(e1id)!!["attr1"])
        assertEquals("value2", conn1.db.pull(e2id)!!["attr2"])
        assertEquals("value1", conn2.db.pull(e1id)!!["attr1"])
        assertEquals("value2", conn2.db.pull(e2id)!!["attr2"])
    }

}
