package qbit.typing

import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.serializersModuleOf
import qbit.api.model.Attr
import qbit.factorization.KSFactorization
import qbit.test.model.*
import kotlin.reflect.KClass

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