package qbit.serialization

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import qbit.api.QBitException
import qbit.api.model.Hash
import qbit.api.model.hash
import qbit.ns.Namespace
import qbit.platform.asInput
import qbit.platform.createSingleThreadCoroutineDispatcher
import qbit.spi.Storage

private val nodes = Namespace("nodes")

class NodesStorage(private val storage: Storage) :
        (NodeRef) -> NodeVal?,
    CoroutineScope by CoroutineScope(createSingleThreadCoroutineDispatcher("Nodes writer")) {

    suspend fun store(n: NodeVal): NodeVal {
        return withContext(this.coroutineContext) {
            val data = SimpleSerialization.serializeNode(n)
            val hash = hash(data)
            if (n.hash != hash) {
                throw AssertionError("NodeVal has hash ${n.hash.toHexString()}, but it's serialization has hash ${hash.toHexString()}")
            }
            if (!storage.hasKey(hash.key())) {
                storage.add(hash.key(), data)
            }
            toHashedNode(n, hash)
        }
    }

    override fun invoke(ref: NodeRef): NodeVal? {
        try {
            val value = storage.load(ref.key()) ?: return null
            val hash = hash(value)
            if (hash != ref.hash) {
                throw QBitException("Corrupted node. Node hash is ${ref.hash}, but data hash is $hash")
            }
            return toHashedNode(SimpleSerialization.deserializeNode(value.asInput()), hash)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun load(n: NodeRef): NodeVal? {
        return invoke(n)
    }

    private fun Node.key() = nodes[hash.toHexString()]

    private fun Hash.key() = nodes[toHexString()]

    private fun toHashedNode(n: NodeVal, hash: Hash): NodeVal = when (n) {
        is Root -> Root(hash, n.source, n.timestamp, n.data)
        is Leaf -> Leaf(hash, n.parent, n.source, n.timestamp, n.data)
        is Merge -> Merge(hash, n.parent1, n.parent2, n.source, n.timestamp, n.data)
    }

    fun hasNode(head: Node): Boolean =
            storage.hasKey(head.key())
}
