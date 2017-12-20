package qbit

import qbit.serialization.SimpleSerialization
import qbit.storage.Storage
import java.io.ByteArrayInputStream
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

    private val instanceEid = EID(dbUuid.iid.value, 0)
    private val graph = Graph({ key -> resolve(key) })

    private fun resolve(key: String): NodeVal? {
        try {
            val value = storage.load(key)
            return value?.let { SimpleSerialization.deserializeNode(ByteArrayInputStream(value)) }
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun create(e: Map<String, Any>): EID {
        try {
            val eid = EID(dbUuid.iid.value, pull(instanceEid)!!.get("entities") as Int + 1)
            add(eid, e)
            return eid
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun add(eid: EID, e: Map<String, Any>) {
        try {
            val entity = e.entries.map { Fact(eid, it.key, it.value) } + Fact(instanceEid, "entities", eid.eid)
            store(entity)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun pull(eid: EID): Map<String, Any>? {
        try {
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
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun fork(): Pair<DbUuid, Node> {
        try {
            val forks = pull(instanceEid)!!.get("forks") as Int
            val id = DbUuid(dbUuid.iid.fork(forks + 1))
            val eid = EID(id.iid.value, 0)
            store(Fact(instanceEid, "forks", forks + 1),
                    Fact(eid, "forks", 0),
                    Fact(eid, "entities", 0))
            return Pair(id, head)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun store(e: Fact) {
        try {
            head = add(NodeData(arrayOf(e)))
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun store(e: List<Fact>) {
        try {
            store(*e.toTypedArray())
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun store(vararg e: Fact) {
        try {
            val newHead = add(NodeData(e))
            storage.store(newHead.hash.toHexString(), SimpleSerialization.serializeNode(newHead))
            head = newHead
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun sync(another: Db): Node {
        try {
            val novelty = graph.findSubgraph(head, another.dbUuid)
            val newRoot = another.push(novelty)
            val newHead = graph.append(newRoot).let {
                storeAppendedNodes(it.first)
                it.first
            }
            head = newHead
            return head
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun fetch(source: Db) {
        try {
            val keys = source.storage.keys()
            for (key in keys) {
                val value = source.storage.load(key)
                if (value != null) {
                    storage.store(key, value)
                } else {
                    println("Value for $key not found")
                }
            }
        } catch (e: Exception) {
            throw QBitException("Fetch failed", e)
        }
    }

    private fun push(novetyRoot: Node): Merge {
        val (newHead, mergeRoot) = graph.append(novetyRoot)
        storeAppendedNodes(newHead)
        val myNovelty = graph.findSubgraph(head, mergeRoot)
        head = merge(newHead)
        return Merge(myNovelty, NodeRef(novetyRoot), dbUuid, System.currentTimeMillis(), (head as Merge).data)
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

    private fun add(data: NodeData): Leaf = Leaf(head, dbUuid, System.currentTimeMillis(), data)

    private fun merge(r: Node): Merge = Merge(head, r, dbUuid, System.currentTimeMillis(), NodeData(emptyArray()))

}

