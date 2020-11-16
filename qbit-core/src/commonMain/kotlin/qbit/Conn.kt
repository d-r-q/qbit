package qbit

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
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
import qbit.api.theInstanceEid
import qbit.factoring.Factor
import qbit.factoring.serializatoin.KSFactorizer
import qbit.index.Indexer
import qbit.index.InternalDb
import qbit.ns.Namespace
import qbit.resolving.HasConflictResult
import qbit.resolving.hasConflict
import qbit.resolving.resolveConflicts
import qbit.serialization.*
import qbit.spi.Storage
import qbit.storage.SerializedStorage
import qbit.trx.*
import kotlin.reflect.KClass

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


    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
    ) {
        TODO("Not yet implemented")
    }

    override fun <Base : Any> polymorphicDefault(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (className: String?) -> DeserializationStrategy<out Base>?
    ) {
        TODO("Not yet implemented")
    }

}

suspend fun qbit(storage: Storage, appSerialModule: SerializersModule): Conn {
    val serializedStorage = SerializedStorage(storage)
    val iid = Iid(1, 4)
    val dbUuid = DbUuid(iid)
    val headHash = storage.load(Namespace("refs")["head"])
    val systemSerialModule = qbitSerialModule + appSerialModule
    systemSerialModule.dumpTo(SchemaValidator())
    return if (headHash != null) {
        val head = CommonNodesStorage(storage).load(NodeRef(Hash(headHash)))
            ?: throw QBitException("Corrupted head: no such node")
        // TODO: fix dbUuid retrieving
        QConn(
            systemSerialModule,
            dbUuid,
            serializedStorage,
            head,
            KSFactorizer(systemSerialModule)::factor
        )
    } else {
        bootstrap(serializedStorage, dbUuid, KSFactorizer(systemSerialModule)::factor, systemSerialModule)
    }
}

class QConn(
    serialModule: SerializersModule,
    override val dbUuid: DbUuid,
    val storage: Storage,
    head: NodeVal<Hash>,
    private val factor: Factor
) : Conn(), CommitHandler {

    private val nodesStorage = CommonNodesStorage(storage)

    var trxLog: TrxLog = QTrxLog(head, HashMap(), Writer(nodesStorage, dbUuid))

    private val resolveNode = nodesResolver(nodesStorage)

    private var db: InternalDb = Indexer(serialModule, null, null, resolveNode).index(head)

    override val head
        get() = trxLog.hash

    override fun db() = db

    override fun db(body: (Db) -> Unit) {
        body(db)
    }

    override fun trx(): Trx {
        return QTrx(db.pull(Gid(dbUuid.iid, theInstanceEid))!!, trxLog, db, this, factor)
    }

    override suspend fun <R : Any> persist(e: R): WriteResult<R?> {
        return with(trx()) {
            val wr = persist(e)
            commit()
            wr
        }
    }

    override suspend fun update(trxLog: TrxLog, newLog: TrxLog, newDb: InternalDb) {
        val conflictResult = hasConflict(this.trxLog, trxLog)
        if (conflictResult.result == HasConflictResult.CONFLICT) {
            resolveConflicts()
        }
        storage.overwrite(Namespace("refs")["head"], newLog.hash.bytes)
        this.trxLog = newLog
        this.db = newDb
    }

}

private fun nodesResolver(nodeStorage: NodesStorage): (Node<Hash>) -> NodeVal<Hash> = { n ->
    when (n) {
        is NodeVal<Hash> -> n
        is NodeRef -> nodeStorage.load(n) ?: throw QBitException("Corrupted graph, could not resolve $n")
    }
}

