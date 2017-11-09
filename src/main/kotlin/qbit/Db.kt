package qbit

import java.util.*

private typealias Trx = Array<Fact>

fun Db(): Db {
    val iid = IID(0, 4)
    val dbUuid = DbUuid(iid)
    val g = Graph(Root(dbUuid, System.currentTimeMillis()), dbUuid)
    val d = Db(dbUuid, g)
    d.store(Fact(d.instanceEid, "forks", 0),
            Fact(d.instanceEid, "entities", 0))
    return d
}

@Suppress("UNCHECKED_CAST")
class Db(private val dbUuid: DbUuid, graph: Graph) {

    private var log = graph
    internal val instanceEid = EID(dbUuid.iid.value, 0)

    fun create(e: Map<String, Any>): EID {
        val eid = EID(dbUuid.iid.value, pull(instanceEid)!!.get("entities") as Int + 1)
        add(eid, e)
        return eid
    }

    fun add(eid: EID, e: Map<String, Any>) {
        val entity = e.entries.map { Fact(eid, it.key, it.value) } + Fact(instanceEid, "entities", eid.eid)
        store(entity)
    }

    fun pull(eid: EID): Map<String, Any>? {
        val res = HashMap<String, Any>()
        log.walk {
            if (it is DataHolder) {
                it.data.trx.filter { it.entityId == eid }.forEach {
                    res.putIfAbsent(it.attribute, it.value)
                }
            }
            false
        }
        return res
    }

    fun fork(): Db {
        val forks = pull(instanceEid)!!.get("forks") as Int
        val id = DbUuid(dbUuid.iid.fork(forks + 1))
        val eid = EID(id.iid.value, 0)
        store(Fact(instanceEid, "forks", forks + 1),
                Fact(eid, "forks", 0),
                Fact(eid, "entities", 0))
        return Db(id, Graph(log.head, id))
    }

    fun store(e: Fact) {
        log = log.add(NodeData(arrayOf(e)))
    }

    fun store (e: List<Fact>) {
        store(*e.toTypedArray())
    }

    fun store(vararg e: Fact) {
        log = log.add(NodeData(e))
    }

    fun sync(another: Db) {
        val novelty = log.findSubgraph(log.head, another.dbUuid)
        val newRoot = another.push(novelty)
        log = Graph(log.append(newRoot)?.first!!, dbUuid)
    }

    private fun push(novetyRoot: N): Merge {
        val (appendNovelty, mergeRoot) = log.append(novetyRoot) ?: throw AssertionError("Could not append $log to $log")
        val myNovelty = log.findSubgraph(log.head, mergeRoot)
        log = log.merge(appendNovelty)
        return Merge(myNovelty, Link(novetyRoot), log.head.source, log.head.timestamp, (log.head as Merge).data)
    }

}

