package qbit.trx

import qbit.api.QBitException
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.platform.currentTimeMillis
import qbit.resolving.findBaseNode
import qbit.serialization.*
import kotlin.math.max


interface TrxLog {

    val hash: Hash

    suspend fun append(facts: Collection<Eav>): TrxLog

    suspend fun mergeWith(trxLog: TrxLog, mergeBase: Hash, eavs: Collection<Eav>): TrxLog

    fun nodesSince(to: Hash, resolveNode: (Node<Hash>) -> NodeVal<Hash>?): List<NodeVal<Hash>>

    fun getNodesDepth(): Map<Hash, Int>

}

class QTrxLog(
    internal val head: NodeVal<Hash>,
    internal val nodesDepth: Map<Hash, Int>,
    private val storage: NodesStorage,
    private val dbUuid: DbUuid
) : TrxLog {

    override val hash = head.hash

    suspend fun store(node: NodeVal<Hash?>): NodeVal<Hash> {
        try {
            if (!storage.hasNode(head)) {
                throw QBitException("Could not store child for node with hash=${head.hash}, because it's not exists in the storage")
            }
            return storage.store(node)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    private fun updateMapOfNodesDepth(newHead: NodeVal<Hash>, depthAnotherParent: Int = 0): Map<Hash, Int> {
        val depth = when (newHead) {
            is Merge -> max(nodesDepth.getValue(newHead.parent1.hash), depthAnotherParent) + 1
            is Leaf -> nodesDepth.getValue(newHead.parent.hash) + 1
            else -> throw AssertionError("Should never happen $newHead is Root")
        }
        return nodesDepth.plus(Pair(newHead.hash, depth))
    }

    override suspend fun append(facts: Collection<Eav>): TrxLog {
        val newHead = store(Leaf(null, head, dbUuid, currentTimeMillis(), NodeData(facts.toTypedArray())))
        val newNodesDepth = updateMapOfNodesDepth(newHead)
        return QTrxLog(newHead, newNodesDepth, storage, dbUuid)
    }

    override suspend fun mergeWith(
        trxLog: TrxLog,
        mergeBase: Hash,
        eavs: Collection<Eav>,
    ): TrxLog {
        val newHead = store(
            Merge(
                null, NodeRef(mergeBase), head, NodeRef(trxLog.hash),
                dbUuid, currentTimeMillis(), NodeData(eavs.toTypedArray())
            )
        )
        val trxLogNodesDepth = trxLog.getNodesDepth().filter { !nodesDepth.keys.contains(it.key) }
        val newNodesDepth = updateMapOfNodesDepth(newHead,
            nodesDepth[trxLog.hash]?:trxLog.getNodesDepth()[trxLog.hash]?:throw AssertionError("Should never happen with depth for node: ${trxLog.hash}"))
            .plus(trxLogNodesDepth)
        return QTrxLog(newHead, newNodesDepth, storage, dbUuid)
    }

    override fun nodesSince(to: Hash, resolveNode: (Node<Hash>) -> NodeVal<Hash>?): List<NodeVal<Hash>> {
        return nodesBetween(head, to, resolveNode)
    }

    private fun nodesBetween(from: NodeVal<Hash>, to: Hash, resolveNode: (Node<Hash>) -> NodeVal<Hash>?): List<NodeVal<Hash>>{
        val fromNodes = arrayListOf(from)
        val nodesBetween = mutableListOf<NodeVal<Hash>>()
        while (fromNodes.isNotEmpty()) {
            val fromNode = fromNodes.removeLast()
            when {
                fromNode.hash == to -> break
                fromNode is Root -> continue
                fromNode is Leaf -> {
                    nodesBetween.add(0, fromNode)
                    val fromVal = resolveNode(fromNode.parent)
                        ?: throw QBitException("Corrupted transaction graph, could not load transaction ${fromNode.hash}")
                    fromNodes.add(fromVal)
                }
                fromNode is Merge -> {
                    nodesBetween.add(0, fromNode)
                    val parent1 = resolveNode(fromNode.parent1)
                        ?: throw QBitException("Corrupted transaction graph, could not load transaction ${from.hash}")
                    val parent2 = resolveNode(fromNode.parent2)
                        ?: throw QBitException("Corrupted transaction graph, could not load transaction ${from.hash}")

                    val parents1 = nodesBetween(parent1, fromNode.base.hash, resolveNode)
                    val parents2 = nodesBetween(parent2, fromNode.base.hash, resolveNode)
                    val index1 = parents1.indexOfLast { it.hash == to }
                    val index2 = parents2.indexOfLast { it.hash == to }

                    when {
                        index1 > -1 -> {
                            nodesBetween.addAll(0, parents1.subList(index1, parents1.size))
                            break
                        }
                        index2 > -1 -> {
                            nodesBetween.addAll(0, parents2.subList(index2, parents2.size))
                            break
                        }
                        else -> {
                            val fromVal = resolveNode(fromNode.base)
                                ?: throw QBitException("Corrupted transaction graph, could not load transaction ${from.hash}")
                            nodesBetween.addAll(parents1 + parents2)
                            fromNodes.add(fromVal)
                        }
                    }
                }
                else -> throw AssertionError("Should never happen, from: $fromNode")
            }
        }
        return nodesBetween.toList()
    }

    override fun getNodesDepth(): Map<Hash, Int> {
        return nodesDepth
    }
}

