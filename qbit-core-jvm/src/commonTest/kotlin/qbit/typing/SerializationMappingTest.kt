package qbit.typing

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.modules.serializersModuleOf
import qbit.api.model.Attr
import qbit.factorization.KSFactorization
import qbit.test.model.*
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
    TheSimplestEntity::class to TheSimplestEntity.serializer(),
    MUser::class to MUser.serializer(),
    NullableScalar::class to NullableScalar.serializer(),
    EntityWithRef::class to EntityWithRef.serializer(),
    EntityWithScalarList::class to EntityWithScalarList.serializer(),
    ListOfNullablesHolder::class to ListOfNullablesHolder.serializer(),
    NullableList::class to NullableList.serializer(),
    ResearchGroup::class to ResearchGroup.serializer(),
    Scientist::class to Scientist.serializer()
)

class SerializationMappingTest : MappingTest(KSFactorization(serializersModuleOf(serializers))::ksDestruct)