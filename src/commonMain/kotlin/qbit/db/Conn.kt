package qbit.db

import qbit.*
import qbit.collections.EmptyIterator
import qbit.trx.Db
import qbit.index.Indexer
import qbit.index.pullT
import qbit.factorization.destruct
import qbit.model.Gid
import qbit.model.Iid
import qbit.model.toFacts
import qbit.model.tombstone
import qbit.ns.Namespace
import qbit.platform.currentTimeMillis
import qbit.serialization.*
import qbit.serialization.NodesStorage
import qbit.storage.Storage
import qbit.system.DbUuid
import qbit.system.Instance
import qbit.trx.*
import qbit.util.Hash

fun qbit(storage: Storage): Conn {
    val iid = Iid(1, 4)
    val dbUuid = DbUuid(iid)
    val headHash = storage.load(Namespace("refs")["head"])
    return if (headHash != null) {
        val head = NodesStorage(storage).load(NodeRef(Hash(headHash)))
                ?: throw QBitException("Corrupted head: no such node")
        // TODO: fix dbUuid retrieving
        QConn(dbUuid, storage, head)
    } else {
        bootstrap(storage, dbUuid)
    }
}

interface Conn {

    val dbUuid: DbUuid

    fun db(): Db

    fun db(body: (Db) -> Unit)

    fun trx(): Trx

    fun <R : Any> persist(e: R): WriteResult<R?>

    val head: Hash

}

fun nodesResolver(nodeStorage: NodesStorage): (Node<Hash>) -> NodeVal<Hash> = { n ->
    when (n) {
        is NodeVal<Hash> -> n
        is NodeRef -> nodeStorage.load(n) ?: throw QBitException("Corrupted graph, could not resolve $n")
    }
}

internal class QConn(override val dbUuid: DbUuid, val storage: Storage, head: NodeVal<Hash>) : Conn, CommitHandler {

    private val nodesStorage = NodesStorage(storage)

    var trxLog: TrxLog = QTrxLog(head, Writer(nodesStorage, dbUuid))

    private val resolveNode = nodesResolver(nodesStorage)

    private var db: Db = Indexer(null, null, resolveNode).index(head)

    override val head
        get() = trxLog.hash

    override fun db() = db

    override fun db(body: (Db) -> Unit) {
        body(db)
    }

    override fun trx(): Trx {
        return QTrx(db.pullT(Gid(dbUuid.iid, theInstanceEid))!!, trxLog, db, this)
    }

    override fun <R : Any> persist(e: R): WriteResult<R?> {
        return with(trx()) {
            val wr = persist(e)
            commit()
            wr
        }
    }

    override fun update(trxLog: TrxLog, newLog: TrxLog, newDb: Db) {
        if (this.trxLog != trxLog) {
            throw ConcurrentModificationException("Concurrent transactions isn't supported yet")
        }
        this.trxLog = newLog
        db = newDb
        storage.overwrite(Namespace("refs")["head"], newLog.hash.bytes)
    }

}

internal fun bootstrap(storage: Storage, dbUuid: DbUuid): Conn {
    val trx = listOf(Attrs.name, Attrs.type, Attrs.unique, Attrs.list, Instances.iid, Instances.forks, Instances.nextEid, tombstone)
            .flatMap { it.toFacts() }
            .plus(destruct(Instance(Gid(Iid(1, 4), theInstanceEid), 1, 0, firstInstanceEid), bootstrapSchema::get, EmptyIterator))

    val root = Root(null, dbUuid, currentTimeMillis(), NodeData(trx.toTypedArray()))
    val storedRoot = NodesStorage(storage).store(root)
    storage.add(Namespace("refs")["head"], storedRoot.hash.bytes)
    return QConn(dbUuid, storage, storedRoot)
}

