package qbit.index

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.modules.SerializersModule
import qbit.api.QBitException
import qbit.api.model.Hash
import qbit.serialization.Node
import qbit.serialization.NodeRef
import qbit.serialization.NodeVal
import qbit.serialization.impl

class Indexer(
    private val serialModule: SerializersModule,
    private val base: IndexDb?,
    private val baseHash: Hash?,
    val resolveNode: (Node<Hash>) -> NodeVal<Hash>?,
) {

    suspend fun index(from: Node<Hash>): IndexDb {
        val fromVal = resolveNode(from)
            ?: throw QBitException("Corrupted transaction graph, could not load transaction ${from.hash}")
        val nodes = flow {
            impl(
                when (baseHash) {
                    null -> null
                    else -> NodeRef(baseHash)
                },
                fromVal, resolveNode
            )
        }.toList()
        return nodes.fold(base ?: IndexDb(Index(), serialModule)) { db, n ->
            val entities = n.data.trxes.toList()
                .groupBy { it.gid }
                .map { it.key to it.value }
            IndexDb(db.index.add(entities), serialModule)
        }
    }

}

