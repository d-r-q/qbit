package qbit.typing

import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.serializersModuleOf
import qbit.factorization.KSFactorization
import qbit.test.model.TheSimplestEntity
import qbit.test.model.EntityWithRef
import qbit.test.model.EntityWithRefList
import qbit.test.model.EntityWithScalarList
import kotlin.reflect.KClass

private val serializers: Map<KClass<*>, KSerializer<*>> = mapOf(
    TheSimplestEntity::class to TheSimplestEntity.serializer(),
    EntityWithRef::class to EntityWithRef.serializer(),
    EntityWithScalarList::class to EntityWithScalarList.serializer(),
    EntityWithRefList::class to EntityWithRefList.serializer()
)

class SerializationFactorizationTest :
    CommonFactorizationTest(KSFactorization(serializersModuleOf(serializers))::ksDestruct)
