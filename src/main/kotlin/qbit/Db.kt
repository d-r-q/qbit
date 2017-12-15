package qbit

import qbit.serialization.SimpleSerialization
import qbit.storage.Storage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*

fun Db(storage: Storage): Db {
    val iid = IID(0, 4)
    val dbUuid = DbUuid(iid)
    val g = Root(dbUuid, System.currentTimeMillis(), NodeData(arrayOf(
            Fact(EID(iid.value, 0), "forks", 0),
            Fact(EID(iid.value, 0), "entities", 0))))
    storage.store(g.hash.toHexString(), SimpleSerialization.serializeNode(g))
    return Db(storage, dbUuid, g)
}


@Suppress("UNCHECKED_CAST")
class Db(private val storage: Storage, private val dbUuid: DbUuid, private var head: Node) {

    internal val instanceEid = EID(dbUuid.iid.value, 0)
    private val graph = Graph({ key -> resolve(key) })

    fun resolve(key: String): Try<NodeVal?> {
        val value = storage.load(key)
        return if (value.isOk) {
            if (value.res != null) SimpleSerialization.deserializeNode(ByteArrayInputStream(value.res))
            else ok(null)
        } else value as Try<NodeVal?>
    }

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
        graph.walk(head) {
            if (it is NodeVal) {
                it.data.trx.filter { it.entityId == eid }.forEach {
                    res.putIfAbsent(it.attribute, it.value)
                }
            }
            false
        }
        return res.takeIf { it.size > 0 }
    }

    fun fork(): Pair<DbUuid, Node> {
        val forks = pull(instanceEid)!!.get("forks") as Int
        val id = DbUuid(dbUuid.iid.fork(forks + 1))
        val eid = EID(id.iid.value, 0)
        store(Fact(instanceEid, "forks", forks + 1),
                Fact(eid, "forks", 0),
                Fact(eid, "entities", 0))
        return Pair(id, head)
    }

    fun store(e: Fact) {
        head = add(NodeData(arrayOf(e)))
    }

    fun store(e: List<Fact>) {
        store(*e.toTypedArray())
    }

    fun store(vararg e: Fact) {
        val newHead = add(NodeData(e))
        storage.store(newHead.hash.toHexString(), SimpleSerialization.serializeNode(newHead))
        head = newHead
    }

    fun sync(another: Db): Try<Node> {
        val novelty = graph.findSubgraph(head, another.dbUuid)
        val newRoot = ifOk(novelty) { another.push(it) }
        val newHead = ifOk(newRoot) { nr ->
            graph.append(nr).mapOk {
                storeAppendedNodes(it.first)
                it.first
            }
        }
        return newHead.mapOk {
            head = it
            head
        }
    }

    private fun push(novetyRoot: Node): Try<Merge> {
        val appendTry = graph.append(novetyRoot)
        appendTry.mapOk { storeAppendedNodes(it.first) }
        val myNovelty = appendTry.mapOk { (_, mergeRoot) -> graph.findSubgraph(head, mergeRoot) }
        return ifOk(appendTry, myNovelty) { a, m ->
            head = merge(a.first)
            Merge(m, NodeRef(novetyRoot), dbUuid, System.currentTimeMillis(), (head as Merge).data)
        }
    }

    private fun storeAppendedNodes(it: NodeVal) {
        graph.walk(it) { n ->
            when (n) {
                is NodeRef -> true
                is NodeVal -> {
                    storage.store(n.hash.toHexString(), SimpleSerialization.serializeNode(n))
                    false
                }
            }
        }
    }

    fun add(data: NodeData): Leaf = Leaf(head, dbUuid, System.currentTimeMillis(), data)

    fun merge(r: Node): Merge = Merge(head, r, dbUuid, System.currentTimeMillis(), NodeData(emptyArray()))
}

