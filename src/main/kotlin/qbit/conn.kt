package qbit

import qbit.ns.Namespace
import qbit.schema.*
import qbit.storage.NodesStorage
import qbit.storage.Storage
import java.util.*
import java.util.Collections.singleton
import java.util.Collections.singletonList

fun qbit(storage: Storage): LocalConn {
    val iid = IID(1, 4)
    val dbUuid = DbUuid(iid)
    val headHash = storage.load(Namespace("refs")["head"])
    if (headHash != null) {
        val head = NodesStorage(storage).load(NodeRef(Hash(headHash)))
                ?: throw QBitException("Corrupted head: no such node")
        // TODO: fix dbUuid retrieving
        return LocalConn(dbUuid, storage, head)
    }

    var eid = 0
    var trx = listOf(Fact(EID(iid.value, eid), qbit.schema._name, qbit.schema._name.str()),
            Fact(EID(iid.value, eid), qbit.schema._type, QString.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, true))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), qbit.schema._name, qbit.schema._type.str()),
            Fact(EID(iid.value, eid), qbit.schema._type, QByte.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, false))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), qbit.schema._name, qbit.schema._unique.str()),
            Fact(EID(iid.value, eid), qbit.schema._type, QBoolean.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, false))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), qbit.schema._name, _forks.str()),
            Fact(EID(iid.value, eid), qbit.schema._type, _forks.type.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, _forks.unique))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), qbit.schema._name, _entities.str()),
            Fact(EID(iid.value, eid), qbit.schema._type, _entities.type.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, _entities.unique))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), qbit.schema._name, _iid.str()),
            Fact(EID(iid.value, eid), qbit.schema._type, _iid.type.code),
            Fact(EID(iid.value, eid), qbit.schema._unique, _iid.unique))
    eid++
    trx += listOf(
            Fact(EID(iid.value, eid), qbit.schema._iid, 0),
            Fact(EID(iid.value, eid), qbit.schema._forks, 0),
            Fact(EID(iid.value, eid), qbit.schema._entities, eid + 1)) // + 1 - is current (instance) entity

    val root = Root(null, dbUuid, System.currentTimeMillis(), NodeData(trx.toTypedArray()))
    val storedRoot = NodesStorage(storage).store(root)
    storage.add(Namespace("refs")["head"], storedRoot.hash.bytes)
    return LocalConn(dbUuid, storage, storedRoot)
}

interface Conn {

    val dbUuid: DbUuid

    var head: NodeVal<Hash>

    fun fork(): Pair<DbUuid, NodeVal<Hash>>

    fun push(noveltyRoot: Node<Hash>): Merge<Hash>

}

class LocalConn(override val dbUuid: DbUuid, val storage: Storage, override var head: NodeVal<Hash>) : Conn {

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
        db = if (newHead is Merge) {
            IndexDb(Index(graph, newHead))
        } else {
            IndexDb(db.index.add(newHead.data.trx.toList()))
        }

        storage.overwrite(Namespace("refs")["head"], newHead.hash.bytes)
    }

    override fun fork(): Pair<DbUuid, NodeVal<Hash>> {
        try {
            val forks = db.pull(instanceEid)!![_forks] ?: throw QBitException("Corrupted database metadata")
            val forkId = DbUuid(dbUuid.iid.fork(forks + 1))
            val forkInstanceEid = EID(forkId.iid.value, 0)
            val newHead = writer.store(
                    head,
                    Fact(instanceEid, qbit.schema._iid, forks + 1),
                    Fact(forkInstanceEid, qbit.schema._forks, 0),
                    Fact(forkInstanceEid, qbit.schema._entities, 1))
            swapHead(newHead)
            return Pair(forkId, newHead)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun persist(e: Entity): WriteResult {
        return persist(singleton(e))
    }

    fun persist(vararg es: Entity): WriteResult =
            persist(es.asList())

    fun persist(es: Collection<Entity>): WriteResult {
        try {
            // TODO: check for conflict modifications in parallel threads
            val curEntities = db.pull(instanceEid)?.get(_entities) ?: throw QBitException("Corrupted database metadata")
            val eids = EID(dbUuid.iid.value, curEntities).nextEids()
            val allEs = unfold(es, eids)
            val storedEs = allEs
                    .filter { it.key !is StoredEntity || (it.key as StoredEntity).dirty }
                    .map {
                        var res: IdentifiedEntity = it.key as? StoredEntity ?: it.value
                        res.entries.forEach { e ->
                            if (e.key is RefAttr) {
                                val ra: RefAttr = e.key as RefAttr
                                val re: Entity = e.value as Entity
                                res = res.set(ra, allEs[re]!!)
                            }
                        }
                        res
                    }
            var facts = storedEs.flatMap { it.toFacts() }
            if (facts.isEmpty()) {
                qbit.assert { es.all { it is StoredEntity && !it.dirty } }
                return WriteResult(db, es.filterIsInstance<StoredEntity>(), emptyMap())
            }
            val newEntities = eids.next().eid
            if (curEntities < newEntities) {
                facts += Fact(instanceEid, qbit.schema._entities, newEntities)
            }
            validate(db, facts)
            swapHead(writer.store(head, facts))
            return WriteResult(db, es.map {
                it as? StoredEntity ?: Entity(allEs[it]!!.eid, db)
            }.toList(), allEs.filterKeys { it !is StoredEntity }.mapValues { Entity(it.value.eid, db) })
        } catch (qe: QBitException) {
            throw qe
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    private fun unfold(es: Collection<Entity>, eids: Iterator<EID>): IdentityHashMap<Entity, IdentifiedEntity> {
        val res = IdentityHashMap<Entity, IdentifiedEntity>()

        fun body(es: Collection<Entity>) {
            es.forEach {
                if (!res.contains(it)) {
                    if (it is IdentifiedEntity) {
                        res[it] = it
                    } else {
                        res[it] = it.toIdentified(eids.next())
                    }
                }
                it.entries.forEach { e ->
                    if (e.key is RefAttr) {
                        val value: Entity = e.value as Entity
                        if (res[value] == null) {
                            body(singletonList(value))
                        }
                    }
                }
            }
        }
        body(es)

        return res
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
        val myNovelty = Graph.findSubgraph(head, Graph.refs(noveltyRoot).asSequence().map { it.hash }.toSet())
        val head = writer.appendNode(merge(head, newDb))
        swapHead(head)
        return Merge(head.hash, myNovelty, NodeRef(noveltyRoot), dbUuid, head.timestamp, head.data)
    }

    private fun merge(head1: Node<Hash>, head2: Node<Hash>): Merge<Hash?> =
            Merge(null, head1, head2, dbUuid, System.currentTimeMillis(), NodeData(emptyArray()))

}

class WriteResult(val db: Db, val persistedEntities: List<StoredEntity>, val generatedEntities: Map<Entity, StoredEntity>) {

    operator fun component1(): Db = db

    operator fun component2(): Map<Entity, StoredEntity> = generatedEntities

    operator fun component3(): StoredEntity = persistedEntities[0]

    operator fun component4(): StoredEntity = persistedEntities[1]

    operator fun component5(): StoredEntity = persistedEntities[2]

    operator fun component6(): StoredEntity = persistedEntities[3]

    operator fun component7(): StoredEntity = persistedEntities[4]

    operator fun component8(): StoredEntity = persistedEntities[5]

    fun storedEntity(): StoredEntity =
            persistedEntities[0]

}