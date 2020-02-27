package qbit.typing

import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.serializersModuleOf
import qbit.factorization.KSFactorization
import qbit.test.model.*
import kotlin.reflect.KClass

private val serializers: Map<KClass<*>, KSerializer<*>> = mapOf(
    TheSimplestEntity::class to TheSimplestEntity.serializer(),
    EntityWithRef::class to EntityWithRef.serializer(),
    EntityWithScalarList::class to EntityWithScalarList.serializer(),
    EntityWithRefList::class to EntityWithRefList.serializer(),
    ListOfNullablesHolder::class to ListOfNullablesHolder.serializer(),
    ListOfNullables::class to ListOfNullables.serializer(),
    NullableList::class to NullableList.serializer(),
    MUser::class to MUser.serializer(),
    ResearchGroup::class to ResearchGroup.serializer(),
    Scientist::class to Scientist.serializer(),
    Country::class to Country.serializer(),
    NullableScalar::class to NullableScalar.serializer(),
    Bomb::class to Bomb.serializer(),
    NullableIntEntity::class to NullableIntEntity.serializer(),
    EntityWithRefToNullableInt::class to EntityWithRefToNullableInt.serializer(),
    ByteArrayEntity::class to ByteArrayEntity.serializer(),
    ListOfByteArraysEntity::class to ListOfByteArraysEntity.serializer(),
    GidEntity::class to GidEntity.serializer(),
    NotNullableGidEntity::class to NotNullableGidEntity.serializer()
)

private val attrsMap = serializers
    .flatMap { readSchema(it.value.descriptor) }
    .map { it.name to it }
    .toMap()


class SerializationFactorizationTest :
    CommonFactorizationTest(KSFactorization(serializersModuleOf(serializers))::ksDestruct, attrsMap)
