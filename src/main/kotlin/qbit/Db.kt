package qbit

import qbit.serialization.SimpleSerialization
import qbit.storage.Namespace
import qbit.storage.Storage

val nodes = Namespace("nodes")

fun Db(storage: Storage): Db {
    val iid = IID(0, 4)
    val dbUuid = DbUuid(iid)
    val g = Root(dbUuid, System.currentTimeMillis(), NodeData(arrayOf(
            Fact(EID(iid.value, 0), "forks", 0),
            Fact(EID(iid.value, 0), "entities", 0))))
    storage.store(nodes[g.hash.toHexString()], SimpleSerialization.serializeNode(g))
    return Db(storage, dbUuid, g)
}


@Suppress("UNCHECKED_CAST")
class Db(private val dbUuid: DbUuid) {

    fun sync(another: Db, anotherUuid: DbUuid, thiz: Writer): Db2 {
        try {
            val novelty = thiz.db.findSubgraph(anotherUuid)
            val newRoot = another.push(novelty)
            return thiz.append(newRoot)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun fetch(source: Db2, thiz: Writer): Db2 {
        try {
            return thiz.append(source.head)
        } catch (e: Exception) {
            throw QBitException("Fetch failed", e)
        }
    }

    private fun push(dst: Db2, novetyRoot: Node, thiz: Writer): Merge {
        val newDb = thiz.append(novetyRoot)
        val myNovelty = dst.findSubgraph(head, mergeRoot)
        head = merge(newHead)
        return Merge(myNovelty, NodeRef(novetyRoot), dbUuid, System.currentTimeMillis(), (head as Merge).data)
    }

    private fun merge(r: Node): Merge = Merge(head, r, dbUuid, System.currentTimeMillis(), NodeData(emptyArray()))

}

