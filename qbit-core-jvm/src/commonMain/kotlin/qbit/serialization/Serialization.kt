package qbit.serialization

import io.ktor.utils.io.core.Input
import qbit.api.model.Hash
import qbit.api.system.DbUuid

interface Serialization {

    fun serializeNode(n: NodeVal): ByteArray

    fun deserializeNode(ins: Input): NodeVal

}

class DeserializationException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
