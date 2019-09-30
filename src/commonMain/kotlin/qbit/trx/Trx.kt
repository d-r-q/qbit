package qbit.trx

import qbit.*
import qbit.Attrs.list
import qbit.Attrs.name
import qbit.Attrs.type
import qbit.Attrs.unique
import qbit.Instances.forks
import qbit.Instances.nextEid
import qbit.mapping.destruct
import qbit.model.*
import qbit.ns.Namespace
import qbit.platform.IdentityHashMap
import qbit.platform.currentTimeMillis
import qbit.storage.NodesStorage
import qbit.storage.Storage


interface Trx {

    val db: Db

    fun <R : Any> persist(entityGraphRoot: R): WriteResult<R>

    fun commit()

    fun rollback()

}

internal class QbitTrx2(private val inst: Instance, private val trxLog: TrxLog, private var base: Db, private val conn: InternalConn) : Trx {

    private var curDb: Db? = null

    private val factsBuffer = ArrayList<Fact>()

    val eids = Gid(inst.iid, inst.nextEid).nextGids()

    override val db
        get() = (this.curDb ?: this.base)

    override fun <R : Any> persist(entityGraphRoot: R): WriteResult<R> {
        val facts = destruct(entityGraphRoot, db::attr, eids)
        factsBuffer.addAll(facts)
        curDb = db.with(facts)
        return QbitWriteResult(Any() as R, curDb!!, IdentityHashMap<Any, Any>())
    }

    override fun commit() {
        val newLog = trxLog.append(factsBuffer + destruct(inst.copy(nextEid = eids.next().eid), curDb!!::attr, EmptyIterator))
        try {
            conn.update(trxLog, newLog)
            base = curDb!!
            factsBuffer.clear()
        } catch (e: Throwable) {
            // todo clean up
            throw e
        }
    }

    override fun rollback() {
        factsBuffer.clear()
        curDb = null
    }

}

interface WriteResult<R> {

    val persisted: R

    val db: Db

    val createdEntities: IdentityHashMap<Any, Any>

}

internal data class QbitWriteResult<R>(
        override val persisted: R,
        override val db: Db,
        override val createdEntities: IdentityHashMap<Any, Any>
) : WriteResult<R>

internal interface TrxLog {

    val hash: Hash

    fun append(facts: Collection<Fact>): TrxLog

}

internal class QbitTrxLog(val head: NodeVal<Hash>, val writer: Writer) : TrxLog {

    override val hash = head.hash

    override fun append(facts: Collection<Fact>): TrxLog {
        val newHead = writer.store(head, facts)
        return QbitTrxLog(newHead, writer)
    }

}

data class Instance(val id: Gid, val iid: Int, val forks: Int, val nextEid: Int)

object EmptyIterator : Iterator<Nothing> {

    override fun hasNext(): Boolean {
        return false
    }

    override fun next(): Nothing {
        throw NoSuchElementException("There is no elements in empty iterator")
    }
}

interface Conn {

    val dbUuid: DbUuid

    fun db(body: (Db) -> Unit)

    fun trx(): Trx

}

internal interface InternalConn : Conn {

    fun update(trxLog: TrxLog, newLog: TrxLog)

}

private const val instanceEid = 8

internal class QbitConn(override val dbUuid: DbUuid, val storage: Storage, head: NodeVal<Hash>) : InternalConn {

    private val nodesStorage = NodesStorage(storage)

    private val graph = Graph(nodesStorage)

    private var trxLog: TrxLog = QbitTrxLog(head, Writer(nodesStorage, dbUuid))

    private var db = IndexDb(Index(graph, head))

    override fun db(body: (Db) -> Unit) {
        body(db)
    }

    override fun trx(): Trx {
        return QbitTrx2(db.pullT(Gid(dbUuid.iid, instanceEid))!!, trxLog, db, this)
    }

    override fun update(trxLog: TrxLog, newLog: TrxLog) {
        if (this.trxLog != trxLog) {
            throw ConcurrentModificationException("Concurrent transactions isn't supported yet")
        }
        this.trxLog = newLog
        db = indexTrxLog(db, graph, NodeRef(newLog.hash), trxLog.hash)
    }

}

internal fun indexTrxLog(base: IndexDb, graph: Graph, from: Node<Hash>, upTo: Hash): IndexDb {
    return if (from is Merge) {
        IndexDb(Index(graph, from))
    } else {
        fun nodesBetween(from: NodeVal<Hash>, to: Hash): List<NodeVal<Hash>> {
            return when {
                from.hash == to -> emptyList()
                from is Leaf -> nodesBetween(graph.resolveNode(from.parent), to) + from
                from is Merge -> throw UnsupportedOperationException("Merges not yet supported")
                else -> {
                    check(from is Root)
                    throw AssertionError("Should never happen")
                }
            }
        }

        val nodes = nodesBetween(graph.resolveNode(from), upTo)
        return nodes.fold(base) { db, n ->
            val entities = n.data.trx.toList()
                    .groupBy { it.eid }
                    .map { it.key to it.value }
            IndexDb(db.index.add(entities))
        }
    }
}

fun qbit(storage: Storage): Conn {
    val iid = IID(1, 4)
    val dbUuid = DbUuid(iid)
    val headHash = storage.load(Namespace("refs")["head"])
    if (headHash != null) {
        val head = NodesStorage(storage).load(NodeRef(Hash(headHash)))
                ?: throw QBitException("Corrupted head: no such node")
        // TODO: fix dbUuid retrieving
        return QbitConn(dbUuid, storage, head)
    }

    val trx = listOf(name, type, unique, list, Instances.iid, forks, nextEid)
            .flatMap { it.toFacts() }
            .plus(destruct(Instance(Gid(1, instanceEid), 1, 0, 8), bootstrapSchema::get, EmptyIterator))

    val root = Root(null, dbUuid, currentTimeMillis(), NodeData(trx.toTypedArray()))
    val storedRoot = NodesStorage(storage).store(root)
    storage.add(Namespace("refs")["head"], storedRoot.hash.bytes)
    return QbitConn(dbUuid, storage, storedRoot)
}
