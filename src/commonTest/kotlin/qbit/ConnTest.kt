package qbit

import qbit.model.Fact
import qbit.model.Gid
import qbit.model.IID
import qbit.ns.Namespace
import qbit.platform.Files
import qbit.platform.currentTimeMillis
import qbit.storage.FileSystemStorage
import qbit.storage.MemStorage
import qbit.storage.NodesStorage
import qbit.db.DbUuid
import qbit.db.QConn
import qbit.index.IndexDb
import qbit.serialization.Leaf
import qbit.serialization.NodeData
import qbit.serialization.Root
import kotlin.test.Test


class ConnTest {

    @Test
    fun `Test update conn`() {
        val storage = MemStorage()
        val dbUuid = DbUuid(IID(0, 4))
        val nodesStorage = NodesStorage(storage)

        val root = Root(null, dbUuid, currentTimeMillis(), NodeData(emptyArray()))
        val storedRoot = nodesStorage.store(root)
        val leaf = Leaf(null, storedRoot, dbUuid, currentTimeMillis(), NodeData(arrayOf(Fact(Gid(0, 0), "any", "any"))))
        val storedLeaf = nodesStorage.store(leaf)
        storage.add(Namespace("refs")["head"], storedLeaf.hash.bytes)

        val conn = QConn(dbUuid, storage, storedRoot)

        val newLog = FakeTrxLog(storedLeaf.hash)
        conn.update(conn.trxLog, newLog, emptyDb)

        assertArrayEquals(newLog.hash.bytes, storage.load(Namespace("refs")["head"]))
    }

    @Test
    fun `Transaction committing should update connection's head`() {
        val root = Files.createTempDirectory("qbit").toFile()
        val storage = FileSystemStorage(root)

        val conn = setupTestSchema(storage)
        conn.persist(IntEntity(0, 1))

        val loaded = storage.load(Namespace("refs")["head"])
        val hash = conn.head.bytes
        assertArrayEquals(loaded, hash)
    }

}