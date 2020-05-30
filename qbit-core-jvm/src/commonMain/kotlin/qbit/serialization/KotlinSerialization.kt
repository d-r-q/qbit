package qbit.serialization

import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.readBytes
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.cbor.Cbor


object KotlinSerialization : Serialization {

    override fun serializeNode(n: NodeVal): ByteArray {
        return Cbor.dump(NodeVal.serializer(), n)
    }

    override fun deserializeNode(ins: Input): NodeVal {
        return Cbor.load(NodeVal.serializer(), ins.readBytes())
    }

}

class AnySerializer(override val descriptor: SerialDescriptor) : KSerializer<Any> {

    override fun serialize(encoder: Encoder, value: Any) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): Any {
        TODO("Not yet implemented")
    }

}