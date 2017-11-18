package qbit.serialization

import qbit.*
import java.io.InputStream

interface Serialization {
    fun serializeNode(n: NodeVal): ByteArray
    fun serializeNode(parent1: Node, parent2: Node, source: DbUuid, timestamp: Long, data: NodeData): ByteArray
    fun deserializeNode(ins: InputStream): Try<NodeVal>
}

sealed class DeserializationErr(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
class DeserializationIOErr(msg: String? = null, cause: Throwable? = null) : DeserializationErr(msg, cause)
class DeserializationUnknownMarkErr(msg: String? = null, cause: Throwable? = null) : DeserializationErr(msg, cause)
class DeserializationUnexpectedMarkErr(msg: String? = null, cause: Throwable? = null) : DeserializationErr(msg, cause)
