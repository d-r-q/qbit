package qbit.storage

import qbit.*
import qbit.serialization.SimpleSerialization
import java.io.ByteArrayInputStream

private val nodes = Namespace("nodes")

class NodesStorage(private val storage: Storage) : (NodeRef) -> NodeVal? {

    fun store(n: NodeVal) {
        if (!storage.hasKey(n.key())) {
            storage.add(n.key(), SimpleSerialization.serializeNode(n))
        }
    }

    override fun invoke(ref: NodeRef): NodeVal? {
        try {
            val value = storage.load(ref.key())
            return value?.let { SimpleSerialization.deserializeNode(ByteArrayInputStream(value)) }
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun load(n: NodeRef): NodeVal? {
        return invoke(n)
    }

    private fun Node.key() = nodes[hash.toHexString()]

}
