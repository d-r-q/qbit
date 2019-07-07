package qbit.storage

import qbit.*
import qbit.ns.Namespace
import qbit.platform.asInput
import qbit.serialization.SimpleSerialization

private val nodes = Namespace("nodes")

class NodesStorage(private val storage: Storage) : (NodeRef) -> NodeVal<Hash>? {

    fun store(n: NodeVal<Hash?>): NodeVal<Hash> {
        val data = SimpleSerialization.serializeNode(n)
        val hash = hash(data)
        if (n.hash != null && n.hash != hash) {
            throw AssertionError("NodeVal has hash ${n.hash.toHexString()}, but it's serialization has hash ${hash.toHexString()}")
        }
        if (!storage.hasKey(hash.key())) {
            storage.add(hash.key(), data)
        }
        return toHashedNode(n, hash)
    }

    override fun invoke(ref: NodeRef): NodeVal<Hash>? {
        try {
            val value = storage.load(ref.key()) ?: return null
            val hash = hash(value)
            if (hash != ref.hash) {
                throw QBitException("Corrupted node. Node hash is ${ref.hash}, but data hash is $hash")
            }
            return toHashedNode(SimpleSerialization.deserializeNode(value.asInput()) , hash)
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun load(n: NodeRef): NodeVal<Hash>? {
        return invoke(n)
    }

    private fun Node<Hash>.key() = nodes[hash.toHexString()]

    private fun Hash.key() = nodes[toHexString()]

    private fun toHashedNode(n: NodeVal<Hash?>, hash: Hash): NodeVal<Hash> = when (n) {
        is Root -> Root(hash, n.source, n.timestamp, n.data)
        is Leaf -> Leaf(hash, n.parent, n.source, n.timestamp, n.data)
        is Merge -> Merge(hash, n.parent1, n.parent2, n.source, n.timestamp, n.data)
    }

    fun hasNode(head: Node<Hash>): Boolean =
            storage.hasKey(head.key())
}
