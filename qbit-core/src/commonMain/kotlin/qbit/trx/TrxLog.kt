package qbit.trx

import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.serialization.*
import kotlin.math.max


interface TrxLog {

    val hash: Hash

    suspend fun append(facts: Collection<Eav>): TrxLog

    fun nodesSince(hash: Hash): Sequence<NodeVal<Hash>>

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
            is Root -> 0
        }
        val newNodesDepth = nodesDepth.plus(Pair(newHead, depth))
        return QTrxLog(newHead, newNodesDepth, writer)
    }

    override fun nodesSince(hash: Hash): Sequence<NodeVal<Hash>> {
        TODO("Not yet implemented")
    }

}

