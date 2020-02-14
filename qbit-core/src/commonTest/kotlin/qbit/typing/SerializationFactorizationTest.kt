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
    ListOfNullables::class to ListOfNullables.serializer()
)

class SerializationFactorizationTest :
    CommonFactorizationTest(KSFactorization(serializersModuleOf(serializers))::ksDestruct)
