package qbit

import qbit.api.gid.Gid
import qbit.api.gid.Iid
import qbit.api.model.Eav
import qbit.api.system.DbUuid
import qbit.ns.Namespace
import qbit.platform.currentTimeMillis
import qbit.platform.runBlocking
import qbit.serialization.Leaf
import qbit.serialization.NodeData
import qbit.serialization.NodesStorage
import qbit.serialization.Root
import qbit.storage.MemStorage
import qbit.test.model.IntEntity
import qbit.test.model.testsSerialModule
import kotlin.test.Test


class ConnTest {

    @Test
    fun `Test head ref in storage is updated after updating connection head`() {
        runBlocking {
            val storage = MemStorage()
            val dbUuid = DbUuid(Iid(0, 4))
            val nodesStorage = NodesStorage(storage)

            val root = Root(null, dbUuid, currentTimeMillis(), NodeData(emptyArray()))
            val storedRoot = nodesStorage.store(root)
            val leaf =
                Leaf(null, storedRoot, dbUuid, currentTimeMillis(), NodeData(arrayOf(Eav(Gid(0, 0), "any", "any"))))
            val storedLeaf = nodesStorage.store(leaf)
            storage.add(Namespace("refs")["head"], storedLeaf.hash.bytes)

            val conn = QConn(testsSerialModule, dbUuid, storage, storedRoot, testSchemaFactorizer::factor)

            val newLog = FakeTrxLog(storedLeaf.hash)
            conn.update(conn.trxLog, newLog, EmptyDb)

            assertArrayEquals(newLog.hash.bytes, storage.load(Namespace("refs")["head"]))
        }
    }

    @Test
    fun `Transaction committing should update connection's head`() {
        runBlocking {
            val storage = MemStorage()

            val conn = setupTestSchema(storage)
            conn.persist(IntEntity(0, 1))

            val loaded = storage.load(Namespace("refs")["head"])
            val hash = conn.head.bytes
            assertArrayEquals(loaded, hash)
        }
    }

}