package qbit

import qbit.api.QBitException
import qbit.api.db.Conn
import qbit.api.db.Db
import qbit.api.db.Trx
import qbit.api.db.WriteResult
import qbit.api.gid.Gid
import qbit.api.gid.Iid
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.api.theInstanceEid
import qbit.index.Indexer
import qbit.index.pullT
import qbit.ns.Namespace
import qbit.serialization.Node
import qbit.serialization.NodeRef
import qbit.serialization.NodeVal
import qbit.serialization.NodesStorage
import qbit.spi.Storage
import qbit.trx.CommitHandler
import qbit.trx.QTrx
import qbit.trx.QTrxLog
import qbit.trx.TrxLog
import qbit.trx.Writer

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

internal class QConn(override val dbUuid: DbUuid, val storage: Storage, head: NodeVal<Hash>) : Conn(), CommitHandler {

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

