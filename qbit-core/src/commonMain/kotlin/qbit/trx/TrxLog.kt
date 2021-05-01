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

    suspend fun mergeWith(trxLog: TrxLog, mergeBase: Hash, eavs: Collection<Eav>): TrxLog

    fun nodesSince(to: Hash): List<Node<Hash>>

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

    private fun updateMapOfNodesDepth(newHead: NodeVal<Hash>, nodeDepth: (Hash) -> Int): Map<Hash, Int> {
        val depth = when (newHead) {
            is Merge -> max(nodesDepth.getValue(newHead.parent1.hash), nodeDepth(newHead.parent2.hash)) + 1
            is Leaf -> nodesDepth.getValue(newHead.parent.hash) + 1
            else -> throw AssertionError("Should never happen $newHead is Root")
        }
        return nodesDepth.plus(Pair(newHead.hash, depth))
    }

    override suspend fun append(facts: Collection<Eav>): TrxLog {
        val newHead = store(Leaf(null, head, dbUuid, currentTimeMillis(), NodeData(facts.toTypedArray())))
        val newNodesDepth = updateMapOfNodesDepth(newHead) { 0 }
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
        val newNodesDepth = updateMapOfNodesDepth(newHead,trxLog.getNodesDepth()::getValue)
            .plus(trxLogNodesDepth)
        return QTrxLog(newHead, newNodesDepth, storage, dbUuid)
    }

    override fun nodesSince(to: Hash): List<Node<Hash>> {
        return nodesBetween(head, to)
    }

    private fun nodesBetween(from: Node<Hash>, to: Hash): List<Node<Hash>>{
        return when {
            from.hash == to -> emptyList()
            from is Root -> emptyList()
            from is Leaf -> {
                nodesBetween(from.parent,to) + from
            }
            from is Merge -> {
                val parents1 = nodesBetween(from.parent1, from.base.hash)
                val parents2 = nodesBetween(from.parent2, from.base.hash)
                val index1 = parents1.indexOfLast { it.hash == to }
                val index2 = parents2.indexOfLast { it.hash == to }

                return when {
                    index1 > -1 -> parents1.subList(index1,parents1.size)
                    index2 > -1 -> parents2.subList(index2,parents2.size)
                    else -> nodesBetween(from.base,to) + parents1 + parents2 + from
                }
            }
            else -> throw AssertionError("Should never happen, from: $from")
        }
    }

    override fun getNodesDepth(): Map<Hash, Int> {
        return nodesDepth
    }
}

