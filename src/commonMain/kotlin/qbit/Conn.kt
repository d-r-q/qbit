package qbit

import qbit.index.Db
import qbit.index.Indexer
import qbit.index.pullT
import qbit.model.*
import qbit.model.Namespace
import qbit.serialization.Node
import qbit.serialization.NodeRef
import qbit.serialization.NodeVal
import qbit.serialization.NodesStorage
import qbit.serialization.Storage
import qbit.trx.*
import qbit.model.Hash
import qbit.model.impl.QBitException

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

private fun nodesResolver(nodeStorage: NodesStorage): (Node<Hash>) -> NodeVal<Hash> = { n ->
    when (n) {
        is NodeVal<Hash> -> n
        is NodeRef -> nodeStorage.load(n) ?: throw QBitException("Corrupted graph, could not resolve $n")
    }
}

