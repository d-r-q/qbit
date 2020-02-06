package qbit.typing

import kotlinx.serialization.*
import kotlinx.serialization.modules.serializersModuleOf
import qbit.NullableScalar
import qbit.api.model.Attr
import qbit.factorization.KSFactorization
import qbit.test.model.Addr
import qbit.test.model.MUser
import kotlin.reflect.KClass

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

private val serializers: Map<KClass<*>, KSerializer<*>> = mapOf(
    Attr::class to Attr.serializer(FakeSerializer<Any>()),
    Addr::class to Addr.serializer(),
    MUser::class to MUser.serializer(),
    NullableScalar::class to NullableScalar.serializer()
)

//class SerializationFactorizationTest : MappingTest(KSFactorization(serializersModuleOf(serializers))::ksDestruct)