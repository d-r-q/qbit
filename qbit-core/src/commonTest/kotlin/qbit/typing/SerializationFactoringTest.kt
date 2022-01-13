package qbit.typing

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleCollector
import qbit.GidEntity
import qbit.api.tombstone
import qbit.factoring.serializatoin.KSFactorizer
import qbit.qbitSerialModule
import qbit.schema.schemaFor
import qbit.test.model.testsSerialModule
import kotlin.reflect.KClass

val qbitCoreTestsSerialModule = SerializersModule {
    include(qbitSerialModule)
    include(testsSerialModule)
    contextual(GidEntity::class, GidEntity.serializer())
    contextual(NotNullableGidEntity::class, NotNullableGidEntity.serializer())
}

val schemaAttrs = (qbitCoreTestsSerialModule).serializers()
    .flatMap { schemaFor(it.value.descriptor) }
    .map { it.name to it }
    .toMap()

private val attrsMap = schemaAttrs + (tombstone.name to tombstone)

class SerializationFactoringTest :
    CommonFactoringTest(KSFactorizer(qbitCoreTestsSerialModule)::factor, attrsMap)

fun SerializersModule.serializers() =
    HashMap<KClass<*>, KSerializer<*>>().apply {
        this@serializers.dumpTo(object : SerializersModuleCollector {
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

            override fun <T : Any> contextual(
                kClass: KClass<T>,
                provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>
            ) {
                TODO("Not yet implemented")
            }

            override fun <Base : Any> polymorphicDefault(
                baseClass: KClass<Base>,
                defaultSerializerProvider: (className: String?) -> DeserializationStrategy<out Base>?
            ) {
            }

            @ExperimentalSerializationApi
            override fun <Base : Any> polymorphicDefaultDeserializer(
                baseClass: KClass<Base>,
                defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<out Base>?
            ) {
                TODO("Not yet implemented")
            }

            @ExperimentalSerializationApi
            override fun <Base : Any> polymorphicDefaultSerializer(
                baseClass: KClass<Base>,
                defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?
            ) {
                TODO("Not yet implemented")
            }
        })
    }
