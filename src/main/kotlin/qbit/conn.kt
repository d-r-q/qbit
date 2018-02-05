package qbit

import qbit.storage.NodesStorage
import qbit.storage.Storage

fun qbit(storage: Storage): LocalConn {
    val iid = IID(0, 4)
    val dbUuid = DbUuid(iid)
    val root = Root(null, dbUuid, System.currentTimeMillis(), NodeData(arrayOf(
            Fact(EID(iid.value, 0), "forks", 0),
            Fact(EID(iid.value, 0), "entities", 0))))
    val storedRoot = NodesStorage(storage).store(root)
    return LocalConn(dbUuid, storage, storedRoot)
}

interface Conn {

    val dbUuid: DbUuid

    val head: NodeVal<Hash>

    fun fork(): Pair<DbUuid, NodeVal<Hash>>

    fun push(noveltyRoot: Node<Hash>): Merge<Hash>

}

class LocalConn(override val dbUuid: DbUuid, storage: Storage, head: NodeVal<Hash>) : Conn {

    override val head: NodeVal<Hash>
        get() = db.head

    private val nodesStorage = NodesStorage(storage)
    var db = Db(head, nodesStorage)

    /**
     * Instance descriptor eid
     * Instance descriptor: {
     *   forks - count of forks created by this instance,
     *   entities - count of entities created by this instance
     * }
     */
    private val instanceEid = EID(dbUuid.iid.value, 0)

    override fun fork(): Pair<DbUuid, NodeVal<Hash>> {
        try {
            val forks = db.pull(instanceEid)!!["forks"] as Int
            val forkId = DbUuid(dbUuid.iid.fork(forks + 1))
            val forkInstanceEid = EID(forkId.iid.value, 0)
            val newHead = writer().store(
                    Fact(instanceEid, "forks", forks + 1),
                    Fact(forkInstanceEid, "forks", 0),
                    Fact(forkInstanceEid, "entities", 0))
            db = Db(newHead, nodesStorage)
            return Pair(forkId, newHead)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun create(e: Map<String, Any>): Pair<Db, EID> {
        try {
            val eid = EID(dbUuid.iid.value, db.pull(instanceEid)!!["entities"] as Int + 1)
            val db = addEntity(eid, e)
            return db to eid
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun addEntity(eid: EID, e: Map<String, Any>): Db {
        try {
            val entity = e.entries.map { Fact(eid, it.key, it.value) } + Fact(instanceEid, "entities", eid.eid)
            db = Db(writer().store(entity), nodesStorage)
            return db
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    private fun writer() = Writer(db, nodesStorage, dbUuid)

    fun sync(another: Conn) {
        try {
            val novelty = db.findSubgraph(another.dbUuid)
            val newRoot = another.push(novelty)
            db = Db(writer().appendGraph(newRoot), nodesStorage)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun fetch(source: Conn) {
        try {
            db = Db(writer().appendGraph(source.head), nodesStorage)
        } catch (e: Exception) {
            throw QBitException("Fetch failed", e)
        }
    }

    override fun push(noveltyRoot: Node<Hash>): Merge<Hash> {
        val newDb = writer().appendGraph(noveltyRoot)
        val myNovelty = db.findSubgraph(db.head, Graph.refs(noveltyRoot).map { it.hash }.toSet())
        val head = writer().appendNode(merge(db.head, newDb))
        db = Db(head, nodesStorage)
        return Merge(head.hash, myNovelty, NodeRef(noveltyRoot), dbUuid, System.currentTimeMillis(), head.data)
    }

    private fun merge(head1: NodeVal<Hash>, head2: NodeVal<Hash>): Merge<Hash?> = Merge(null, head1, head2, head1.source, System.currentTimeMillis(), NodeData(emptyArray()))

}

