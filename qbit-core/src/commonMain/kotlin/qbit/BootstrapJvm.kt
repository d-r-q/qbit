package qbit

import kotlinx.serialization.modules.SerializersModule
import qbit.api.Attrs
import qbit.api.Instances
import qbit.api.db.Conn
import qbit.api.protoInstance
import qbit.api.system.DbUuid
import qbit.api.tombstone
import qbit.factoring.Factor
import qbit.ns.Namespace
import qbit.platform.collections.EmptyIterator
import qbit.platform.currentTimeMillis
import qbit.serialization.CommonNodesStorage
import qbit.serialization.NodeData
import qbit.serialization.Root
import qbit.spi.Storage

suspend fun bootstrap(storage: Storage, dbUuid: DbUuid, factor: Factor, serialModule: SerializersModule): Conn {
    val trx = listOf(Attrs.name, Attrs.type, Attrs.unique, Attrs.list, Instances.iid, Instances.forks, Instances.nextEid, tombstone)
            .flatMap { it.toFacts() }
            .plus(factor(protoInstance, bootstrapSchema::get, EmptyIterator))

    val root = Root(null, dbUuid, currentTimeMillis(), NodeData(trx.toTypedArray()))
    val storedRoot = CommonNodesStorage(storage).store(root)
    storage.add(Namespace("refs")["head"], storedRoot.parentHash.bytes)
    return QConn(serialModule, dbUuid, storage, storedRoot, factor)
}

