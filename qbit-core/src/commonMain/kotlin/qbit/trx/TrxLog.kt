package qbit.trx

import qbit.api.QBitException
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.platform.currentTimeMillis
import qbit.serialization.*
import kotlin.math.max


interface TrxLog {

    val hash: Hash

    suspend fun append(facts: Collection<Eav>): TrxLog

    suspend fun mergeWith(
        trxLog: TrxLog,
        e: Collection<Eav>,
        resolveNode: (Node<Hash>) -> NodeVal<Hash>?
    ): TrxLog

    fun nodesSince(to: Hash, resolveNode: (Node<Hash>) -> NodeVal<Hash>?): List<NodeVal<Hash>>

    fun getNodeDepth(nodeHash: Hash): Int

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
        e: Collection<Eav>,
        resolveNode: (Node<Hash>) -> NodeVal<Hash>?
    ): TrxLog {
        val anotherHead = resolveNode(NodeRef(trxLog.hash))
            ?: throw QBitException("Corrupted transaction graph, could not load transaction ${trxLog.hash}")
        val newHead = store(
            Merge(
                null, head, anotherHead,
                dbUuid, currentTimeMillis(), NodeData(e.toTypedArray())
            )
        )
        val newNodesDepth = updateMapOfNodesDepth(newHead, trxLog.getNodeDepth(anotherHead.hash))
        return QTrxLog(newHead, newNodesDepth, storage, dbUuid)
    }

    override fun nodesSince(to: Hash, resolveNode: (Node<Hash>) -> NodeVal<Hash>?): List<NodeVal<Hash>> {
        val fromNodes = arrayListOf(head)
        val nodesBetween = mutableListOf<NodeVal<Hash>>()
        while (fromNodes.isNotEmpty()) {
            val fromNode = fromNodes.removeLast()
            when {
                fromNode.hash == to -> continue
                fromNode is Root -> continue
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

    override fun getNodeDepth(nodeHash: Hash): Int {
        return nodesDepth.getValue(nodeHash)
    }
}

