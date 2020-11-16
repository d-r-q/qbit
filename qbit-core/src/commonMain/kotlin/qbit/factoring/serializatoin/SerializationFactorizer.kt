@file:Suppress("UNCHECKED_CAST")

package qbit.factoring.serializatoin

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleCollector
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.Eav
import qbit.api.model.Entity
import qbit.api.model.Tombstone
import qbit.api.tombstone
import qbit.collections.IdentityMap
import qbit.factoring.*
import qbit.factoring.AttrName
import qbit.factoring.EntityGraph
import qbit.factoring.Pointer
import qbit.factoring.Ref
import kotlin.reflect.KClass

class KSFactorizer(private val serialModule: SerializersModule) {

    fun factor(e: Any, schema: (String) -> Attr<*>?, gids: Iterator<Gid>): EntityGraphFactoring {
        val encoder = EntityEncoder(
            serialModule,
            EntityGraph(),
            e
        )
        val serializer = (serialModule.getContextual(e::class)
            ?: throw QBitException("Cannot find serializer for $e (${e::class})\nserializers are available for:\n${serialModule.dump()}")) as KSerializer<Any>
        serializer.serialize(encoder, e)
        val eavs = encoder.entityGraph.resolveRefs(schema, gids).map { it.key to it.value.toEavs() }
        return EntityGraphFactoring(IdentityMap(*eavs.toTypedArray()))
    }

}

private fun Entity.toEavs(): List<Eav> =
    when (this) {
        is Tombstone -> listOf(Eav(this.gid, tombstone, true))
        else -> this.keys.flatMap { attr ->
            when (val value = this[attr]) {
                is List<*> -> (value as List<Any>).map { Eav(this.gid, attr, it) }
                else -> listOf(Eav(this.gid, attr, value))
            }
        }
    }

internal class EntityEncoder(
    override val serializersModule: SerializersModule,
    internal val entityGraph: EntityGraph,
    private val obj: Any
) : StubEncoder() {

    init {
        entityGraph.addIfMissing(obj)
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        if (value !is Any) {
            // hint compiler, that value is not null
            return
        }
        if (value in entityGraph.builders) {
            val fieldPointer = Pointer(
                obj,
                AttrName(descriptor, index)
            )
            entityGraph.setAttributeValue(fieldPointer, Ref(value))
            return
        }
        if (obj is Tombstone) {
            entityGraph.addTombstone(obj)
            return
        }


        val values: Any = when (ValueKind.of(
            descriptor,
            index,
            value
        )) {
            ValueKind.SCALAR_VALUE, ValueKind.VALUE_LIST -> {
                value
            }
            ValueKind.SCALAR_REF -> {
                serializeRef(value, serializer)
            }
            ValueKind.REF_LIST -> {
                serializeRefList(value as Iterable<Any>)
            }
        }

        val fieldPointer = Pointer(
            obj,
            AttrName(descriptor, index)
        )
        entityGraph.setAttributeValue(fieldPointer, values)
    }

    private fun serializeRefList(values: Iterable<Any>): List<Ref> =
        values.map { item ->
            val itemSerializer: KSerializer<Any> = (serializersModule.getContextual(item::class) ?: throw QBitException("Cannot find serializer for $item")) as KSerializer<Any>
            serializeRef(item, itemSerializer)
        }

    private fun <T> serializeRef(value: T, serializer: SerializationStrategy<T>): Ref {
        serializer.serialize(
            EntityEncoder(
                serializersModule,
                entityGraph,
                value as Any
            ), value)
        return Ref(value)
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        encodePrimitive(descriptor, index, value)
    }

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        encodePrimitive(descriptor, index, value)
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        encodePrimitive(descriptor, index, value)
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        encodePrimitive(descriptor, index, value)
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        encodePrimitive(descriptor, index, value)
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        encodePrimitive(descriptor, index, value)
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        encodePrimitive(descriptor, index, value)
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        encodePrimitive(descriptor, index, value)
    }

    private fun encodePrimitive(
        descriptor: SerialDescriptor,
        index: Int,
        value: Any
    ) {
        val pointer = Pointer(
            obj,
            AttrName(descriptor, index)
        )
        entityGraph.setAttributeValue(pointer, value)
    }

}

enum class ValueKind {

    SCALAR_VALUE, SCALAR_REF, VALUE_LIST, REF_LIST;

    companion object {
        fun of(descriptor: SerialDescriptor, index: Int, value: Any): ValueKind {
            val elementDescriptor = descriptor.getElementDescriptor(index)
            return when {
                isScalarValue(value) -> {
                    SCALAR_VALUE
                }
                isScalarRef(elementDescriptor) -> {
                    SCALAR_REF
                }
                isValueList(
                    elementDescriptor,
                    value
                ) -> {
                    VALUE_LIST
                }
                isRefList(
                    elementDescriptor,
                    value
                ) -> {
                    REF_LIST
                }
                else -> {
                    throw AssertionError("Writing primitive via encodeSerializableElement")
                }
            }
        }

        private fun isScalarValue(value: Any) =
            // other primitive values are encoded directly via encodeXxxElement
            value is Gid || value is ByteArray

        private fun isScalarRef(elementDescriptor: SerialDescriptor) =
            elementDescriptor.kind == StructureKind.CLASS

        private fun isValueList(elementDescriptor: SerialDescriptor, value: Any) =
            elementDescriptor.kind == StructureKind.LIST &&
                    value is List<*> &&
                    (elementDescriptor.getElementDescriptor(0).kind is PrimitiveKind ||
                            elementDescriptor.getElementDescriptor(0).kind == StructureKind.LIST) // ByteArray

        private fun isRefList(elementDescriptor: SerialDescriptor, value: Any) =
            elementDescriptor.kind == StructureKind.LIST && value is List<*>

    }

}

internal fun AttrName(descriptor: SerialDescriptor, index: Int) =
    AttrName(descriptor.serialName, descriptor.getElementName(index))

private fun SerializersModule.dump(): String {
    val collector = ToStringSerialModuleCollector()
    this.dumpTo(collector)
    return collector.buffer.toString()
}

private class ToStringSerialModuleCollector : SerializersModuleCollector {

    val buffer = StringBuilder()

    override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
        buffer.append("$kClass\n")
    }

    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
    ) {
        buffer.append("$baseClass($actualClass)")
    }

    override fun <Base : Any> polymorphicDefault(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (className: String?) -> DeserializationStrategy<out Base>?
    ) {
        TODO("Not yet implemented")
    }

}

