package qbit.serialization

import qbit.*
import qbit.platform.InputStream

interface Serialization {
    fun serializeNode(n: NodeVal<Hash?>): ByteArray
    fun serializeNode(parent1: Node<Hash>, parent2: Node<Hash>, source: DbUuid, timestamp: Long, data: NodeData): ByteArray
    fun deserializeNode(ins: InputStream): NodeVal<Hash?>
}

class DeserializationException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
