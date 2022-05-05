package qbit

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleCollector
import kotlinx.serialization.modules.plus
import qbit.api.QBitException
import qbit.api.db.*
import qbit.api.gid.Gid
import qbit.api.gid.Iid
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.api.system.Instance
import qbit.api.theInstanceEid
import qbit.factoring.Factor
import qbit.factoring.serializatoin.KSFactorizer
import qbit.index.Indexer
import qbit.index.InternalDb
import qbit.index.RawEntity
import qbit.index.entities
import qbit.ns.Namespace
import qbit.resolving.lastWriterWinsResolve
import qbit.resolving.logsDiff
import qbit.serialization.*
import qbit.spi.Storage
import qbit.storage.SerializedStorage
import qbit.trx.*
import kotlin.reflect.KClass

suspend fun qbit(storage: Storage, appSerialModule: SerializersModule, registerFolders: Map<String, (Any, Any) -> Any>): Conn {
    val iid = Iid(1, 4)
    // TODO: fix dbUuid retrieving
    val dbUuid = DbUuid(iid)

    val serializedStorage = SerializedStorage(storage)
    val nodesStorage = CommonNodesStorage(serializedStorage)
    val systemSerialModule = createSystemSerialModule(appSerialModule)
    val factor = KSFactorizer(systemSerialModule)::factor
    val head = loadOrInitHead(storage, nodesStorage, serializedStorage, dbUuid, factor)
    val db = Indexer(
        systemSerialModule,
        null,
        null,
        nodesResolver(nodesStorage),
        causalHashesResolver(nodesStorage),
        registerFolders
    ).index(head)

    return QConn(dbUuid, serializedStorage, head, factor, nodesStorage, db)
}

private suspend fun loadOrInitHead(
    storage: Storage,
    nodesStorage: CommonNodesStorage,
    serializedStorage: SerializedStorage,
    dbUuid: DbUuid,
    factor: Factor
): NodeVal<Hash> {
    val headHash = storage.load(Namespace("refs")["head"])
    val head =
        if (headHash != null) {
            nodesStorage.load(NodeRef(Hash(headHash))) ?: throw QBitException("Corrupted head: no such node")
        } else {
            bootstrapStorage(serializedStorage, dbUuid, factor)
        }
    return head
}

private fun createSystemSerialModule(appSerialModule: SerializersModule): SerializersModule {
    val systemSerialModule = qbitSerialModule + appSerialModule
    systemSerialModule.dumpTo(SchemaValidator())
    return systemSerialModule
}

class QConn(
    override val dbUuid: DbUuid,
    val storage: Storage,
    head: NodeVal<Hash>,
    private val factor: Factor,
    nodesStorage: CommonNodesStorage,
    private var db: InternalDb
) : Conn(), CommitHandler {
    var trxLog: TrxLog = QTrxLog(head, mapOf(Pair(head.hash, 0)), nodesStorage, dbUuid)

    private val resolveNode = nodesResolver(nodesStorage)

    private val resolveCausality = causalHashesResolver(nodesStorage)

    private val gidSequence: GidSequence = with(db.pull<Instance>(Gid(dbUuid.iid, theInstanceEid))) {
        if (this == null) {
            throw QBitException("Corrupted DB - the instance entity not found")
        }
        GidSequence(this.iid, this.nextEid)
    }

    override val head
        get() = trxLog.hash

    override fun db() = db

    override fun db(body: (Db) -> Unit) {
        body(db)
    }

    override fun trx(): Trx {
        return QTrx(db.pull(Gid(dbUuid.iid, theInstanceEid))!!, trxLog, db, this, factor, gidSequence, resolveCausality)
    }

    override suspend fun <T> trx(body: Trx.() -> T): T {
        val trx = trx()
        try {
            val res = trx.body()
            trx.commit()
            return res
        } catch (e: Throwable) {
            trx.rollback()
            throw e
        }
    }

    override suspend fun <R : Any> persist(e: R): WriteResult<R?> {
        return with(trx()) {
            val wr = persist(e)
            commit()
            wr
        }
    }

    override suspend fun update(trxLog: TrxLog, newLog: TrxLog, newDb: InternalDb) {
        val (log, db) =
            if (hasConcurrentTrx(trxLog)) {
                mergeLogs(trxLog, this.trxLog, newLog, newDb)
            } else {
                newLog to newDb
            }
        storage.overwrite(Namespace("refs")["head"], newLog.hash.bytes)
        this.trxLog = log
        this.db = db
    }

    private fun hasConcurrentTrx(trxLog: TrxLog) =
        trxLog != this.trxLog

    private suspend fun mergeLogs(
        baseLog: TrxLog,
        committedLog: TrxLog,
        committingLog: TrxLog,
        newDb: InternalDb
    ): Pair<TrxLog, InternalDb> {
        val logsDifference = logsDiff(baseLog, committedLog, committingLog, resolveNode)
        val reconciliationEavs = logsDifference
            .reconciliationEntities(lastWriterWinsResolve { db.attr(it) })
            .toEavsList()

        val mergedLog = committingLog.mergeWith(committedLog, baseLog.hash, reconciliationEavs)
        val allNodes = nodesBetween(null, mergedLog.head, resolveNode).toList()
        val indexedNodes = nodesBetween(null, committingLog.head, resolveNode).toSet()
        val notIndexedNodes = allNodes.filter { node -> indexedNodes.none { it.hash == node.hash } }
        val mergedDb = notIndexedNodes.fold(newDb) {db, n ->
            db.with(n.entities().flatMap { it.second }, n.hash, resolveCausality(n.hash))
        }

        return mergedLog to mergedDb
    }

    private fun List<RawEntity>.toEavsList() =
        flatMap { it.second }

}

private fun nodesResolver(nodeStorage: NodesStorage): (Node<Hash>) -> NodeVal<Hash> = { n ->
    when (n) {
        is NodeVal<Hash> -> n
        is NodeRef -> nodeStorage.load(n) ?: throw QBitException("Corrupted graph, could not resolve $n")
    }
}

fun causalHashesResolver(nodeStorage: NodesStorage): suspend (Hash) -> List<Hash> = { hash ->
    val node = nodeStorage.load(NodeRef(hash)) ?: throw QBitException("Error: could not resolve node for hash $hash")
    val resolveNode = nodesResolver(nodeStorage)
    val causalNodes = nodesBetween(null, node, resolveNode)
    causalNodes.map { it.hash }.toList()
}

@Suppress("EXPERIMENTAL_API_USAGE")
class SchemaValidator : SerializersModuleCollector {

    override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
        validateDescriptor(serializer.descriptor)
    }

    private fun validateDescriptor(desc: SerialDescriptor) {
        val nullableListProps =
            desc.elementDescriptors
                .withIndex()
                .map { (idx, eDescr) -> eDescr to desc.getElementName(idx) }
                .filter { (eDescr, _) -> eDescr.kind == StructureKind.LIST && eDescr.getElementDescriptor(0).isNullable }
                .map { it.second }
        if (nullableListProps.isNotEmpty()) {
            throw QBitException(
                "List of nullable elements is not supported. Properties: ${desc.serialName}.${
                    nullableListProps.joinToString(
                        ",",
                        "(",
                        ")"
                    ) { it }
                }"
            )
        }
    }

    override fun <T : Any> contextual(
        kClass: KClass<T>,
        provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>
    ) {
        TODO("Not yet implemented")
    }


    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
    ) {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun <Base : Any> polymorphicDefaultDeserializer(
        baseClass: KClass<Base>,
        defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<out Base>?
    ) {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun <Base : Any> polymorphicDefaultSerializer(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?
    ) {
        TODO("Not yet implemented")
    }

}
