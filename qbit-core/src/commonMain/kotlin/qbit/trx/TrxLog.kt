package qbit.trx

import qbit.api.QBitException
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.serialization.*
import kotlin.math.max


interface TrxLog {

    val hash: Hash

    suspend fun append(facts: Collection<Eav>): TrxLog

    fun nodesSince(to: Hash, resolveNode: (Node<Hash>) -> NodeVal<Hash>?): List<NodeVal<Hash>>

}

class QTrxLog(
    internal val head: NodeVal<Hash>,
    internal val nodesDepth: Map<Node<Hash>, Int>,
    private val writer: Writer
) : TrxLog {

    override val hash = head.hash

    override suspend fun append(facts: Collection<Eav>): TrxLog {
        val newHead = writer.store(head, facts)
        val depth = when (newHead) {
            is Merge -> max(nodesDepth.getValue(newHead.parent1), nodesDepth.getValue(newHead.parent2)) + 1
            is Leaf -> nodesDepth.getValue(newHead.parent) + 1
            else -> throw AssertionError("Should never happen $newHead is Root")
        }
        val newNodesDepth = nodesDepth.plus(Pair(newHead, depth))
        return QTrxLog(newHead, newNodesDepth, writer)
    }

    override fun nodesSince(to: Hash, resolveNode: (Node<Hash>) -> NodeVal<Hash>?): List<NodeVal<Hash>> {
        val fromNodes = arrayListOf(head)
        val nodesBetween = mutableListOf<NodeVal<Hash>>()
        while (fromNodes.isNotEmpty()){
            val fromNode = fromNodes.removeLast()
            when{
                fromNode.hash == to -> {
                    nodesBetween.add(fromNode)
                    continue
                }
                fromNode is Leaf -> {
                    nodesBetween.add(fromNode)
                    val fromVal = resolveNode(fromNode.parent)
                        ?: throw QBitException("Corrupted transaction graph, could not load transaction ${fromNode.hash}")
                    fromNodes.add(fromVal)
                }
                fromNode is Merge -> throw UnsupportedOperationException("Merges not yet supported")
                else -> throw AssertionError("Should never happen, from: $fromNode")
            }
        }
        return nodesBetween.toList()
    }
}

