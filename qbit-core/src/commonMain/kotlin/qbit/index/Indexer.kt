package qbit.index

import kotlinx.coroutines.flow.toList
import kotlinx.serialization.modules.SerializersModule
import qbit.api.model.Hash
import qbit.serialization.*

class Indexer(
    private val serialModule: SerializersModule,
    private val base: IndexDb?,
    private val baseNode: Node<Hash>?,
    private val resolveNode: (Node<Hash>) -> NodeVal<Hash>?,
) {

    suspend fun index(from: Node<Hash>): IndexDb {
        return nodesBetween(baseNode, from, resolveNode)
            .toList()
            .map { it.entities() }
            .fold(base ?: IndexDb(Index(), serialModule)) { db, n ->
                db.with(n.flatMap { it.second })
            }
    }

}

private fun NodeVal<Hash>.entities(): List<RawEntity> =
    data.trxes.toList()
        .groupBy { it.gid }
        .map { it.key to it.value }

