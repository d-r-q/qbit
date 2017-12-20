package qbit.serialization

import qbit.DbUuid
import qbit.Node
import qbit.NodeData
import qbit.NodeVal
import java.io.InputStream

interface Serialization {
    fun serializeNode(n: NodeVal): ByteArray
    fun serializeNode(parent1: Node, parent2: Node, source: DbUuid, timestamp: Long, data: NodeData): ByteArray
    fun deserializeNode(ins: InputStream): NodeVal
}

class DeserializationException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
