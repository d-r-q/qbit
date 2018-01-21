package qbit

import qbit.serialization.SimpleSerialization
import qbit.storage.Storage
import java.io.ByteArrayInputStream

class Writer(val db: Db2, private val storage: Storage, private val dbUuid: DbUuid) {

    /**
     * Instance descriptor eid
     * Instance descriptor: {
     *   forks - count of forks created by this instance,
     *   entities - count of entities created by this instance
     * }
     */
    private val instanceEid = EID(dbUuid.iid.value, 0)

    private fun resolve(key: String): NodeVal? {
        try {
            val value = storage.load(nodes[key])
            return value?.let { SimpleSerialization.deserializeNode(ByteArrayInputStream(value)) }
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun fork(): Pair<DbUuid, Node> {
        try {
            val forks = db.pull(instanceEid)!!.get("forks") as Int
            val id = DbUuid(dbUuid.iid.fork(forks + 1))
            val eid = EID(id.iid.value, 0)
            val newDb = store(Fact(instanceEid, "forks", forks + 1),
                    Fact(eid, "forks", 0),
                    Fact(eid, "entities", 0))
            return Pair(id, newDb.head)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun create(e: Map<String, Any>): Pair<Db2, EID> {
        try {
            val eid = EID(dbUuid.iid.value, db.pull(instanceEid)!!.get("entities") as Int + 1)
            val db = addEntity(eid, e)
            return db to eid
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun addEntity(eid: EID, e: Map<String, Any>): Db2 {
        try {
            val entity = e.entries.map { Fact(eid, it.key, it.value) } + Fact(instanceEid, "entities", eid.eid)
            return store(entity)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    private fun store(e: List<Fact>): Db2 {
        try {
            return store(*e.toTypedArray())
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    private fun store(vararg e: Fact): Db2 {
        try {
            val newHead = addNode(NodeData(e))
            storage.store(nodes[newHead.hash.toHexString()], SimpleSerialization.serializeNode(newHead))
            return Db2(newHead, this::resolve)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    private fun addNode(data: NodeData): Leaf = Leaf(db.head, dbUuid, System.currentTimeMillis(), data)

    fun append(n: Node): Db2 {
        return when (n) {
            is NodeRef -> {
                val head = resolve(n.hash.toHexString()) ?: throw QBitException("Could not resolve node $n")
                Db2(head, this::resolve)
            }
            is Leaf -> {
                append(n.parent)
                storage.store(nodes[n.hash.toHexString()], SimpleSerialization.serializeNode(n))
                Db2(n, this::resolve)
            }
            is Merge -> {
                append(n.parent1)
                append(n.parent2)
                storage.store(nodes[n.hash.toHexString()], SimpleSerialization.serializeNode(n))
                Db2(n, this::resolve)
            }
            is Root -> throw QBitException("Could not append root")
        }
    }
}