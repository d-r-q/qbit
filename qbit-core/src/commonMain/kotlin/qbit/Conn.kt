package qbit

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.StructureKind
import kotlinx.serialization.elementDescriptors
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.SerialModuleCollector
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
import qbit.serialization.*
import qbit.spi.Storage
import qbit.trx.CommitHandler
import qbit.trx.QTrx
import qbit.trx.QTrxLog
import qbit.trx.TrxLog
import qbit.trx.Writer
import kotlin.reflect.KClass

class SchemaValidator : SerialModuleCollector {

    override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
        validateDescriptor(serializer.descriptor)
    }

    private fun validateDescriptor(desc: SerialDescriptor) {
        val nullableListProps =
            desc.elementDescriptors()
                .withIndex()
                .map { (idx, eDescr) -> eDescr to desc.getElementName(idx) }
                .filter { (eDescr, _) -> eDescr.kind == StructureKind.LIST && eDescr.getElementDescriptor(0).isNullable }
                .map { it.second }
        if (nullableListProps.isNotEmpty()) {
            throw QBitException(
                "List of nullable elements is not supported. Properties: ${desc.serialName}.${nullableListProps.joinToString(
                    ",",
                    "(",
                    ")"
                ) { it }}"
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

}
suspend fun qbit(storage: Storage, appSerialModule: SerialModule): Conn {
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
            storage,
            head,
            KSFactorizer(systemSerialModule)::factor
        )
    } else {
        bootstrap(storage, dbUuid, KSFactorizer(systemSerialModule)::factor, systemSerialModule)
    }
}

class QConn(serialModule: SerialModule, override val dbUuid: DbUuid, val storage: Storage, head: NodeVal<Hash>, private val factor: Factor) : Conn(), CommitHandler {

    private val nodesStorage = CommonNodesStorage(storage)

    var trxLog: TrxLog = QTrxLog(head, Writer(nodesStorage, dbUuid))

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
        if (this.trxLog != trxLog) {
            throw ConcurrentModificationException("Concurrent transactions isn't supported yet")
        }
        this.trxLog = newLog
        db = newDb
        storage.overwrite(Namespace("refs")["head"], newLog.hash.bytes)
    }

}

private fun nodesResolver(nodeStorage: NodesStorage): (Node<Hash>) -> NodeVal<Hash> = { n ->
    when (n) {
        is NodeVal<Hash> -> n
        is NodeRef -> nodeStorage.load(n) ?: throw QBitException("Corrupted graph, could not resolve $n")
    }
}

