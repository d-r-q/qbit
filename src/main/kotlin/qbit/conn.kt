package qbit

import qbit.EInstance.entitiesCount
import qbit.EInstance.forks
import qbit.EInstance.iid
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
    val trx = mutableListOf(Fact(EID(iid.value, eid), EAttr.name, EAttr.name.str()),
            Fact(EID(iid.value, eid), EAttr.type, QString.code),
            Fact(EID(iid.value, eid), EAttr.unique, true))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), EAttr.name, EAttr.type.str()),
            Fact(EID(iid.value, eid), EAttr.type, QByte.code),
            Fact(EID(iid.value, eid), EAttr.unique, false))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), EAttr.name, EAttr.unique.str()),
            Fact(EID(iid.value, eid), EAttr.type, QBoolean.code),
            Fact(EID(iid.value, eid), EAttr.unique, false))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), EAttr.name, qbit.EAttr.list.str()),
            Fact(EID(iid.value, eid), EAttr.type, QBoolean.code),
            Fact(EID(iid.value, eid), EAttr.unique, false))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), EAttr.name, forks.str()),
            Fact(EID(iid.value, eid), EAttr.type, forks.type.code),
            Fact(EID(iid.value, eid), EAttr.unique, forks.unique))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), EAttr.name, entitiesCount.str()),
            Fact(EID(iid.value, eid), EAttr.type, entitiesCount.type.code),
            Fact(EID(iid.value, eid), EAttr.unique, entitiesCount.unique))
    eid++
    trx += listOf(Fact(EID(iid.value, eid), EAttr.name, EInstance.iid.str()),
            Fact(EID(iid.value, eid), EAttr.type, EInstance.iid.type.code),
            Fact(EID(iid.value, eid), EAttr.unique, EInstance.iid.unique))
    eid++
    trx += listOf(
            Fact(EID(iid.value, eid), EInstance.iid, 0),
            Fact(EID(iid.value, eid), forks, 0),
            Fact(EID(iid.value, eid), entitiesCount, eid + 1)) // + 1 - is current (instance) entity

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
            db.query(hasAttr(entitiesCount))
                    .first { it.eid.iid == dbUuid.iid.value }
                    .eid

    private fun swapHead(newHead: NodeVal<Hash>) {
        head = newHead
        db = if (newHead is Merge) {
            IndexDb(Index(graph, newHead))
        } else {
            val entities = newHead.data.trx.toList()
                    .groupBy { it.eid }
                    .map {
                        if (!it.value[0].deleted) {
                            it.key to it.value
                        } else {
                            it.key to emptyList()
                        }
                    }
            IndexDb(db.index.add(entities))
        }

        storage.overwrite(Namespace("refs")["head"], newHead.hash.bytes)
    }

    override fun fork(): Pair<DbUuid, NodeVal<Hash>> {
        try {

            var instance = db.pull(instanceEid)!!
            val forks = instance[forks]
            val forkId = DbUuid(dbUuid.iid.fork(forks + 1))
            val forkInstanceEid = EID(forkId.iid.value, 0)

            instance = instance.set(EInstance.forks, forks + 1)

            val newInstance = Entity(EInstance.forks eq 0, entitiesCount eq 1, iid eq forkId.iid.value)
            val newHead = writer.store( head, instance.toFacts() + newInstance.toFacts(forkInstanceEid))
            swapHead(newHead)
            return Pair(forkId, newHead)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun persist(e: Entitiable): WriteResult {
        return persist(singleton(e))
    }

    fun persist(vararg es: Entitiable): WriteResult =
            persist(es.asList())

    fun persist(es: Collection<Entitiable>): WriteResult {
        try {
            // TODO: check for conflict modifications in parallel threads

            val instance = db.pull(instanceEid) ?: throw QBitException("Corrupted database metadata")
            val eids = EID(dbUuid.iid.value, instance[entitiesCount]).nextEids()

            val allEs: IdentityHashMap<Entitiable, IdentifiedEntity> = unfoldEntitiesGraph(es, eids)
            val facts: MutableList<Fact> = entitiesToFacts(allEs, eids, instance)
            if (facts.isEmpty()) {
                assert { es.all { it is StoredEntity && !it.dirty } }
                return WriteResult(db, es.filterIsInstance<StoredEntity>(), emptyMap())
            }
            validate(db, facts)
            persistFacts(facts)

            val persistedEntities = es
                    .filter { !((it as? StoredEntity)?.deleted ?: false) }
                    .map { Entity(allEs[it]!!.eid, db) }.toList()
            val createdEntities = allEs.filterKeys { it !is StoredEntity }.mapValues { Entity(it.value.eid, db) }

            return WriteResult(db, persistedEntities, createdEntities)

        } catch (qe: QBitException) {
            throw qe
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    private fun unfoldEntitiesGraph(es: Collection<Entitiable>, eids: Iterator<EID>): IdentityHashMap<Entitiable, IdentifiedEntity> {
        val res = IdentityHashMap<Entitiable, IdentifiedEntity>()

        fun body(es: Collection<Entitiable>) {
            es.forEach {
                if (!res.contains(it)) {
                    when (it) {
                        is IdentifiedEntity -> res[it] = it
                        is Entity -> res[it] = it.toIdentified(eids.next())
                        else -> res[it] = Entity(*it.entries.toTypedArray()).toIdentified(eids.next())
                    }
                }
                it.entries.forEach { e ->
                    if (e is ScalarRefAttrValue) {
                        val value: Entity? = e.value
                        if (value != null && res[value] == null) {
                            body(singletonList(value))
                        }
                    } else if (e is RefListAttrValue) {
                        body(e.value)
                    }
                }
            }
        }
        body(es)

        return res
    }

    private fun entitiesToFacts(allEs: IdentityHashMap<Entitiable, IdentifiedEntity>, eids: Iterator<EID>, instance: StoredEntity): MutableList<Fact> {
        val entitiesToStore: List<IdentifiedEntity> = allEs.values.filter { it !is StoredEntity || it.dirty }
        val linkedEntities: List<IdentifiedEntity> = entitiesToStore.map { it.setRefs(allEs) }
        val facts: MutableList<Fact> = linkedEntities
                .flatMap { it.toFacts() }
                .toMutableList()
        val newEntitiesCnt = eids.next().eid
        if (instance[entitiesCount] < newEntitiesCnt) {
            facts += instance.set(entitiesCount, newEntitiesCnt).toFacts()
        }
        return facts
    }

    private fun persistFacts(facts: MutableList<Fact>) {
        val newHead = writer.store(head, facts)
        swapHead(newHead)
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

class WriteResult(val db: Db, val persistedEntities: List<StoredEntity>, val createdEntities: Map<Entitiable, StoredEntity>) {

    operator fun component1(): Db = db

    operator fun component2(): Map<Entitiable, StoredEntity> = createdEntities

    operator fun component3(): StoredEntity = persistedEntities[0]

    operator fun component4(): StoredEntity = persistedEntities[1]

    operator fun component5(): StoredEntity = persistedEntities[2]

    operator fun component6(): StoredEntity = persistedEntities[3]

    operator fun component7(): StoredEntity = persistedEntities[4]

    operator fun component8(): StoredEntity = persistedEntities[5]

    fun storedEntity(): StoredEntity =
            persistedEntities[0]

}