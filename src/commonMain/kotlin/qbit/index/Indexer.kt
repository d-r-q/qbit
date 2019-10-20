package qbit.index

import qbit.model.impl.QBitException
import qbit.serialization.*
import qbit.model.Hash

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

}

