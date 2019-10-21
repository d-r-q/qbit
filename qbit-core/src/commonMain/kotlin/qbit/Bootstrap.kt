package qbit

import qbit.api.Attrs
import qbit.api.Instances
import qbit.api.db.Conn
import qbit.api.model.Attr
import qbit.api.model.Eav
import qbit.api.protoInstance
import qbit.api.system.DbUuid
import qbit.api.tombstone
import qbit.factorization.destruct
import qbit.ns.Namespace
import qbit.platform.collections.EmptyIterator
import qbit.platform.currentTimeMillis
import qbit.serialization.NodeData
import qbit.serialization.NodesStorage
import qbit.serialization.Root
import qbit.spi.Storage


internal val bootstrapSchema: Map<String, Attr<Any>> = mapOf(
        (Attrs.name.name to Attrs.name) as Pair<String, Attr<Any>>,
        (Attrs.type.name to Attrs.type) as Pair<String, Attr<Any>>,
        (Attrs.unique.name to Attrs.unique) as Pair<String, Attr<Any>>,
        (Attrs.list.name to Attrs.list) as Pair<String, Attr<Any>>,
        (Instances.forks.name to Instances.forks) as Pair<String, Attr<Any>>,
        (Instances.nextEid.name to Instances.nextEid) as Pair<String, Attr<Any>>,
        (Instances.iid.name to Instances.iid) as Pair<String, Attr<Any>>,
        (tombstone.name to tombstone) as Pair<String, Attr<Any>>
)

internal fun bootstrap(storage: Storage, dbUuid: DbUuid): Conn {
    val trx = listOf(Attrs.name, Attrs.type, Attrs.unique, Attrs.list, Instances.iid, Instances.forks, Instances.nextEid, tombstone)
            .flatMap { it.toFacts() }
            .plus(destruct(protoInstance, bootstrapSchema::get, EmptyIterator))

    val root = Root(null, dbUuid, currentTimeMillis(), NodeData(trx.toTypedArray()))
    val storedRoot = NodesStorage(storage).store(root)
    storage.add(Namespace("refs")["head"], storedRoot.hash.bytes)
    return QConn(dbUuid, storage, storedRoot)
}

internal fun Attr<*>.toFacts(): List<Eav> = listOf(Eav(this.id!!, Attrs.name.name, this.name),
        Eav(this.id, Attrs.type.name, this.type),
        Eav(this.id, Attrs.unique.name, this.unique),
        Eav(this.id, Attrs.list.name, this.list))
