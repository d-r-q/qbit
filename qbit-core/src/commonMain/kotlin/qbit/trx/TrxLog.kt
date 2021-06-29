package qbit.trx

import kotlinx.coroutines.flow.*
import qbit.api.QBitException
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.platform.currentTimeMillis
import qbit.serialization.*
import kotlin.math.max


interface TrxLog {

    val hash: Hash
    
    val head: Node<Hash>

    suspend fun append(facts: Collection<Eav>): TrxLog

    suspend fun mergeWith(trxLog: TrxLog, mergeBase: Hash, eavs: Collection<Eav>): TrxLog

    fun nodesAfter(base: Node<Hash>, resolveNode: (Node<Hash>) -> NodeVal<Hash>?): Flow<NodeVal<Hash>>

    fun getNodesDepth(): Map<Hash, Int>

}

class QTrxLog(
    override val head: Node<Hash>,
    private val nodesDepth: Map<Hash, Int>,
    private val storage: NodesStorage,
    private val dbUuid: DbUuid
) : TrxLog {

    override val hash = head.hash

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

    private suspend fun store(node: NodeVal<Hash?>): NodeVal<Hash> {
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

    override fun nodesAfter(base: Node<Hash>, resolveNode: (Node<Hash>) -> NodeVal<Hash>?): Flow<NodeVal<Hash>> =
        nodesBetween(base, head, resolveNode)

    override fun getNodesDepth(): Map<Hash, Int> {
        return nodesDepth
    }
}

