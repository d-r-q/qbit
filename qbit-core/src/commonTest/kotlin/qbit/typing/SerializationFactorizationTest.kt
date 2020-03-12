package qbit.typing

import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.SerialModuleCollector
import kotlinx.serialization.modules.SerializersModule
import qbit.api.tombstone
import qbit.factorization.KSFactorization
import qbit.qbitSerialModule
import qbit.test.model.testsSerialModule
import kotlin.reflect.KClass

private val qbitCoreTestsSerialModule = SerializersModule {
    include(qbitSerialModule)
    include(testsSerialModule)
    contextual(GidEntity::class, GidEntity.serializer())
    contextual(NotNullableGidEntity::class, NotNullableGidEntity.serializer())
}

val schemaAttrs = (qbitCoreTestsSerialModule).serializers()
    .flatMap { readSchema(it.value.descriptor) }
    .map { it.name to it }
    .toMap()

private val attrsMap = schemaAttrs + (tombstone.name to tombstone)


class SerializationFactorizationTest :
    CommonFactorizationTest(KSFactorization(qbitCoreTestsSerialModule)::ksDestruct, attrsMap)

fun SerialModule.serializers() =
    HashMap<KClass<*>, KSerializer<*>>().apply {
        this@serializers.dumpTo(object : SerialModuleCollector {
            override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
                this@apply[kClass] = serializer
            }

            override fun <Base : Any, Sub : Base> polymorphic(
                baseClass: KClass<Base>,
                actualClass: KClass<Sub>,
                actualSerializer: KSerializer<Sub>
            ) {
                this@apply[baseClass] = actualSerializer
            }
        })
    }
