package qbit.index

import qbit.util.Hash
import qbit.QBitException
import qbit.serialization.*

internal class Indexer(private val base: IndexDb?, private val baseHash: Hash?, val resolveNode: (Node<Hash>) -> NodeVal<Hash>?) {

    internal fun index(from: Node<Hash>): IndexDb {
        fun nodesBetween(from: NodeVal<Hash>, to: Hash?): List<NodeVal<Hash>> {
            return when {
                from.hash == to -> emptyList()
                from is Root-> {
                    listOf(from)
                }
                from is Leaf -> {
                    val fromVal = resolveNode(from.parent)
                            ?: throw QBitException("Corrupted transaction graph, could not load transaction ${from.hash}")
                    nodesBetween(fromVal, to) + from
                }
                from is Merge -> throw UnsupportedOperationException("Merges not yet supported")
                else -> throw AssertionError("Should never happen, from: $from")
            }
        }

        val fromVal = resolveNode(from)
                ?: throw QBitException("Corrupted transaction graph, could not load transaction ${from.hash}")
        val nodes = nodesBetween(fromVal, baseHash)
        return nodes.fold(base ?: IndexDb(Index())) { db, n ->
            val entities = n.data.trxes.toList()
                    .groupBy { it.gid }
                    .map { it.key to it.value }
            IndexDb(db.index.add(entities))
        }
    }

/*
    private fun loadFacts(head: NodeVal<Hash>): List<RawEntity> {
        val entities = HashMap<Gid, List<Fact>>()
        val tombstones = HashSet<Gid>()
        var n: NodeVal<Hash>? = head
        while (n != null && n.hash != baseHash) {
            val (removed, toAdd) = n.data.trx.partition { it.attr == tombstone.name }
            tombstones += removed.map { it.eid }.toSet()
            toAdd
                    .filterNot { tombstones.contains(it.eid) || entities.containsKey(it.eid) }
                    .groupBy { it.eid }
                    .forEach {
                        entities[it.key] = it.value
                    }
            n = when (n) {
                is Root -> null
                is Leaf -> resolveNode(n.parent)
                is Merge -> resolveNode(n.parent1)
            }
        }
        return entities.entries
                .map { it.key to it.value }
    }
*/
}

