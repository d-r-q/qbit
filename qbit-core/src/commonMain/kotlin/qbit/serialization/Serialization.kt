package qbit.serialization

import io.ktor.utils.io.core.*
import qbit.api.model.Hash
import qbit.api.system.DbUuid

interface Serialization {

    fun serializeNode(n: NodeVal<Hash?>): ByteArray

    fun serializeNode(
        base: Node<Hash>,
        parent1: Node<Hash>,
        parent2: Node<Hash>,
        source: DbUuid,
        timestamp: Long,
        data: NodeData
    ): ByteArray

    fun deserializeNode(ins: Input): NodeVal<Hash?>

}

class DeserializationException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
