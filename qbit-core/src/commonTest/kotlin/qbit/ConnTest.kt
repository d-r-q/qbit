package qbit

import qbit.api.model.Eav
import qbit.api.gid.Gid
import qbit.api.system.DbUuid
import qbit.api.gid.Iid
import qbit.ns.Namespace
import qbit.platform.Files
import qbit.platform.currentTimeMillis
import qbit.serialization.Leaf
import qbit.serialization.NodeData
import qbit.serialization.NodesStorage
import qbit.serialization.Root
import qbit.storage.FileSystemStorage
import qbit.storage.MemStorage
import kotlin.test.Test


class ConnTest {

    @Test
    fun `Test update conn`() {
        val storage = MemStorage()
        val dbUuid = DbUuid(Iid(0, 4))
        val nodesStorage = NodesStorage(storage)

        val root = Root(null, dbUuid, currentTimeMillis(), NodeData(emptyArray()))
        val storedRoot = nodesStorage.store(root)
        val leaf = Leaf(null, storedRoot, dbUuid, currentTimeMillis(), NodeData(arrayOf(Eav(Gid(0, 0), "any", "any"))))
        val storedLeaf = nodesStorage.store(leaf)
        storage.add(Namespace("refs")["head"], storedLeaf.hash.bytes)

        val conn = QConn(dbUuid, storage, storedRoot)

        val newLog = FakeTrxLog(storedLeaf.hash)
        conn.update(conn.trxLog, newLog, EmptyDb)

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