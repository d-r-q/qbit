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
    private val causalHashesResolver: suspend (Hash) -> List<Hash>,
    private val registerFolders: Map<String, (Any, Any) -> Any>
) {

    suspend fun index(from: Node<Hash>): IndexDb {
        return nodesBetween(baseNode, from, resolveNode)
            .toList()
            .fold(base ?: IndexDb(Index(), serialModule, registerFolders)) { db, n ->
                db.with(n.entities().flatMap { it.second }, n.hash, causalHashesResolver(n.hash))
            }
    }

}

fun NodeVal<Hash>.entities(): List<RawEntity> =
    data.trxes.toList()
        .groupBy { it.gid }
        .map { it.key to it.value }

