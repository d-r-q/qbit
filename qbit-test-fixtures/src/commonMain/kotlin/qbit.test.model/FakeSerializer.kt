package qbit.test.model

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor

class FakeSerializer<T> : KSerializer<T> {

    override val descriptor: SerialDescriptor
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun deserialize(decoder: Decoder): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun serialize(encoder: Encoder, obj: T) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}