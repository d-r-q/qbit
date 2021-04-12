package qbit.index

import kotlinx.serialization.modules.SerializersModule
import qbit.api.QBitException
import qbit.api.model.Hash
import qbit.resolving.findBaseNode
import qbit.serialization.*
import qbit.trx.TrxLog

class Indexer(
    private val serialModule: SerializersModule,
    private val base: IndexDb?,
    private val baseHash: Hash?,
    val resolveNode: (Node<Hash>) -> NodeVal<Hash>?,
) {

    fun index(from: Node<Hash>): IndexDb {
        fun nodesBetween(from: NodeVal<Hash>, to: Hash?): List<NodeVal<Hash>> {
            return when {
                from.hash == to -> emptyList()
                from is Root -> {
                    listOf(from)
                }
                from is Leaf -> {
                    val fromVal = resolveNode(from.parent)
                        ?: throw QBitException("Corrupted transaction graph, could not load transaction ${from.hash}")
                    nodesBetween(fromVal, to) + from
                }
                from is Merge -> {
                    val parent1 = resolveNode(from.parent1)
                        ?: throw QBitException("Corrupted transaction graph, could not load transaction ${from.hash}")
                    val parent2 = resolveNode(from.parent2)
                        ?: throw QBitException("Corrupted transaction graph, could not load transaction ${from.hash}")

                    val parents1 = nodesBetween(parent1, from.base.hash)
                    val parents2 = nodesBetween(parent2, from.base.hash)
                    val index1 = parents1.indexOfLast { it.hash == to }
                    val index2 = parents2.indexOfLast { it.hash == to }


                    return when {
                        index1 > -1 -> {
                            parents1.subList(index1, parents1.size) + from
                        }
                        index2 > -1 -> {
                            parents2.subList(index2, parents2.size) + from
                        }
                        else -> {
                            val fromVal = resolveNode(from.base)
                                ?: throw QBitException("Corrupted transaction graph, could not load transaction ${from.hash}")
                            nodesBetween(fromVal, to) + parents1 + parents2 + from
                        }
                    }
                }
                else -> throw AssertionError("Should never happen, from: $from")
            }
        }

        val fromVal = resolveNode(from)
            ?: throw QBitException("Corrupted transaction graph, could not load transaction ${from.hash}")
        val nodes = nodesBetween(fromVal, baseHash)
        return nodes.fold(base ?: IndexDb(Index(), serialModule)) { db, n ->
            val entities = n.data.trxes.toList()
                .groupBy { it.gid }
                .map { it.key to it.value }
            IndexDb(db.index.add(entities), serialModule)
        }
    }

}

