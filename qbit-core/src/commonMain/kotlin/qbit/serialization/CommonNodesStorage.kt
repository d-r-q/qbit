package qbit.serialization

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import qbit.api.QBitException
import qbit.api.model.Hash
import qbit.ns.Namespace
import qbit.platform.asInput
import qbit.platform.createSingleThreadCoroutineDispatcher
import qbit.serialization.krypto.sha1
import qbit.spi.Storage
import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
private val nodes = Namespace("nodes")

class CommonNodesStorage(private val storage: Storage) :
    CoroutineScope by CoroutineScope(createSingleThreadCoroutineDispatcher("Nodes writer")),
    NodesStorage {

    override suspend fun store(n: NodeVal<Hash?>): NodeVal<Hash> {
        return withContext(this.coroutineContext) {
            val data = SimpleSerialization.serializeNode(n)
            val hash = hash(data)
            if (n.hash != null && n.hash != hash) {
                throw AssertionError("NodeVal has hash ${n.hash.toHexString()}, but it's serialization has hash ${hash.toHexString()}")
            }
            if (!storage.hasKey(hash.key())) {
                storage.add(hash.key(), data)
            }
            toHashedNode(n, hash)
        }
    }

    override fun load(n: Node<Hash>): NodeVal<Hash>? {
        try {
            val value = storage.load(n.key()) ?: return null
            val hash = hash(value)
            if (hash != n.hash) {
                throw QBitException("Corrupted node. Node hash is ${n.hash}, but data hash is $hash")
            }
            return toHashedNode(SimpleSerialization.deserializeNode(value.asInput()), hash)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    private fun Node<Hash>.key() = nodes[hash.toHexString()]

    private fun Hash.key() = nodes[toHexString()]

    private fun toHashedNode(n: NodeVal<Hash?>, hash: Hash): NodeVal<Hash> = when (n) {
        is Root -> Root(hash, n.source, n.timestamp, n.data)
        is Leaf -> Leaf(hash, n.parent, n.source, n.timestamp, n.data)
        is Merge -> Merge(hash, n.base, n.parent1, n.parent2, n.source, n.timestamp, n.data)
    }

    override fun hasNode(head: Node<Hash>): Boolean =
        storage.hasKey(head.key())

}

fun hash(data: ByteArray): Hash =
    Hash(data.sha1().bytes)
