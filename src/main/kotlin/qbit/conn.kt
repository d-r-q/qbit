package qbit

import qbit.ns.Namespace
import qbit.schema.*
import qbit.storage.NodesStorage
import qbit.storage.Storage

fun qbit(storage: Storage): LocalConn {
    val iid = IID(0, 4)
    val dbUuid = DbUuid(iid)

    var eid = 0
    var trx = listOf(Fact(EID(iid.value, eid), qbit.schema._name, qbit.schema._name.str),
            Fact(EID(iid.value, eid), qbit.schema._type, QString.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, true))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), qbit.schema._name, qbit.schema._type.str),
            Fact(EID(iid.value, eid), qbit.schema._type, QByte.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, false))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), qbit.schema._name, qbit.schema._unique.str),
            Fact(EID(iid.value, eid), qbit.schema._type, QBoolean.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, false))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), qbit.schema._name, _forks.str),
            Fact(EID(iid.value, eid), qbit.schema._type, _forks.type.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, _forks.unique))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), qbit.schema._name, _entities.str),
            Fact(EID(iid.value, eid), qbit.schema._type, _entities.type.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, _entities.unique))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), qbit.schema._name, _iid.str),
            Fact(EID(iid.value, eid), qbit.schema._type, _iid.type.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, _iid.unique))
    eid++
    trx += listOf(
            Fact(EID(iid.value, eid), qbit.schema._iid, 0),
            Fact(EID(iid.value, eid), qbit.schema._forks, 0),
            Fact(EID(iid.value, eid), qbit.schema._entities, eid + 1)) // + 1 - is current (instance) entity

    val root = Root(null, dbUuid, System.currentTimeMillis(), NodeData(trx.toTypedArray()))
    val storedRoot = NodesStorage(storage).store(root)
    return LocalConn(dbUuid, storage, storedRoot)
}

interface Conn {

    val dbUuid: DbUuid

    var head: NodeVal<Hash>

    fun fork(): Pair<DbUuid, NodeVal<Hash>>

    fun push(noveltyRoot: Node<Hash>): Merge<Hash>

}

class LocalConn(override val dbUuid: DbUuid, storage: Storage, override var head: NodeVal<Hash>) : Conn {

    private val nodesStorage = NodesStorage(storage)
    private val graph = Graph(nodesStorage)
    private val writer = Writer(nodesStorage, dbUuid)

    var db = IndexDb(Index(graph, head))

    /**
     * Instance descriptor eid
     * Instance descriptor: {
     *   forks - count of forks created by this instance,
     *   entities - count of entities created by this instance
     * }
     */
    private val instanceEid =
            db.query(hasAttr(qbit.schema._entities))
                    .first { it.eid.iid == dbUuid.iid.value }
                    .eid

    private fun swapHead(newHead: NodeVal<Hash>) {
        head = newHead
        db = IndexDb(db.index.add(newHead.data.trx.toList()))
    }

    override fun fork(): Pair<DbUuid, NodeVal<Hash>> {
        try {
            val forks = db.pull(instanceEid)!![_forks] as Int
            val forkId = DbUuid(dbUuid.iid.fork(forks + 1))
            val forkInstanceEid = EID(forkId.iid.value, 0)
            val newHead = writer.store(
                    head,
                    Fact(instanceEid, qbit.schema._iid, forks + 1),
                    Fact(forkInstanceEid, qbit.schema._forks, 0),
                    Fact(forkInstanceEid, qbit.schema._entities, 0))
            swapHead(newHead)
            return Pair(forkId, newHead)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun persist(e: Entity): Pair<Db, StoredEntity> =
            persist(*arrayOf(e))
                    .map { db, list -> db to list[0] }

    fun persist(vararg es: Entity): Pair<Db, List<StoredEntity>> {
        try {
            val curEntities = db.pull(instanceEid)!![_entities] as Int
            val eids = EID(dbUuid.iid.value, curEntities).nextEids()
            val storedEs = es.map {
                it as? StoredEntity ?: it.toStored(eids.next())
            }
            var facts = storedEs.flatMap { it.toFacts() }
            val newEntities = eids.next().eid
            if (curEntities < newEntities) {
                facts += Fact(instanceEid, qbit.schema._entities, newEntities)
            }
            validate(db, facts)
            swapHead(writer.store(head, facts))
            return db to storedEs
        } catch (qe: QBitException) {
            throw qe
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun sync(another: Conn) {
        try {
            val novelty = graph.findSubgraph(head, another.dbUuid)
                    ?: throw QBitException("Could not find node with source = ${another.dbUuid}")
            val newRoot = another.push(novelty)
            swapHead(writer.appendGraph(newRoot))
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun fetch(source: Conn) {
        try {
            swapHead(writer.appendGraph(source.head))
        } catch (e: Exception) {
            throw QBitException("Fetch failed", e)
        }
    }

    override fun push(noveltyRoot: Node<Hash>): Merge<Hash> {
        val newDb = writer.appendGraph(noveltyRoot)
        val myNovelty = Graph.findSubgraph(head, Graph.refs(noveltyRoot).map { it.hash }.toSet())
        val head = writer.appendNode(merge(head, newDb))
        swapHead(head)
        return Merge(head.hash, myNovelty, NodeRef(noveltyRoot), dbUuid, System.currentTimeMillis(), head.data)
    }

    private fun merge(head1: NodeVal<Hash>, head2: NodeVal<Hash>): Merge<Hash?> =
            Merge(null, head1, head2, head1.source, System.currentTimeMillis(), NodeData(emptyArray()))

    private fun <T1, T2, T3, T4> Pair<T1, T2>.map(m: (T1, T2) -> Pair<T3, T4>) =
            m(this.first, this.second)
}

