package qbit.typing

import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.serializersModuleOf
import qbit.factorization.KSFactorization
import qbit.test.model.Addr
import qbit.test.model.UserWithAddr
import kotlin.reflect.KClass

private val serializers: Map<KClass<*>, KSerializer<*>> = mapOf(
    Addr::class to Addr.serializer(),
    UserWithAddr::class to UserWithAddr.serializer())

class SerializationFactorizationTest :
    CommonFactorizationTest(KSFactorization(serializersModuleOf(serializers))::ksDestruct)
