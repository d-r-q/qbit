package qbit

import kotlinx.serialization.modules.plus
import qbit.api.Attrs
import qbit.api.Instances
import qbit.api.gid.Gid
import qbit.api.gid.Iid
import qbit.api.model.Eav
import qbit.api.protoInstance
import qbit.api.system.DbUuid
import qbit.index.Indexer
import qbit.ns.Namespace
import qbit.platform.collections.EmptyIterator
import qbit.platform.currentTimeMillis
import qbit.platform.runBlocking
import qbit.serialization.CommonNodesStorage
import qbit.serialization.Leaf
import qbit.serialization.NodeData
import qbit.serialization.Root
import qbit.storage.MemStorage
import qbit.test.model.IntEntity
import qbit.test.model.testsSerialModule
import kotlin.js.JsName
import kotlin.test.Test


class ConnTest {

    @JsName("Test_head_ref_in_storage_is_updated_after_updating_connection_head")
    @Test
    fun `Test head ref in storage is updated after updating connection head`() {
        runBlocking {
            val storage = MemStorage()
            val dbUuid = DbUuid(Iid(1, 4))
            val nodesStorage = CommonNodesStorage(storage)
            val trx = listOf(
                Attrs.name,
                Attrs.type,
                Attrs.unique,
                Attrs.list,
                Instances.iid,
                Instances.forks,
                Instances.nextEid,
                qbit.api.tombstone
            )
                .flatMap { it.toFacts() }
                .plus(testSchemaFactorizer.factor(protoInstance, bootstrapSchema::get, EmptyIterator))

            val root = Root(null, dbUuid, currentTimeMillis(), NodeData(trx.toTypedArray()))
            val storedRoot = nodesStorage.store(root)
            val leaf =
                Leaf(null, storedRoot, dbUuid, currentTimeMillis(), NodeData(arrayOf(Eav(Gid(1, 10), "any", "any"))))
            val storedLeaf = nodesStorage.store(leaf)
            storage.overwrite(Namespace("refs")["head"], storedLeaf.hash.bytes)

            val conn = QConn(
                dbUuid,
                storage,
                storedRoot,
                testSchemaFactorizer::factor,
                nodesStorage,
                Indexer(qbitSerialModule + testsSerialModule, null, null, testNodesResolver(nodesStorage)).index(storedRoot)
            )

            val newLog = FakeTrxLog(storedLeaf.hash)
            conn.update(conn.trxLog, EmptyDb, newLog, EmptyDb)

            assertArrayEquals(newLog.hash.bytes, storage.load(Namespace("refs")["head"]))
        }
    }

    @JsName("Transaction_committing_should_update_connections_head")
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