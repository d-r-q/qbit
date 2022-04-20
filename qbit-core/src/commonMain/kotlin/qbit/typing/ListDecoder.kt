package qbit.typing

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.StoredEntity
import qbit.factoring.serializatoin.AttrName

@Suppress("UNCHECKED_CAST")
class ListDecoder(
    val schema: (String) -> Attr<*>?,
    val entity: StoredEntity,
    private val elements: List<Any>,
    override val serializersModule: SerializersModule,
    private val cache: HashMap<Gid, Any?>
) : StubDecoder() {

    private var isRefList = false

    private var indexCounter = 0

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val elementDescriptor = descriptor.getElementDescriptor(0)
        val elementKind = elementDescriptor.kind

        isRefList = when {
            isValueAttr(elementDescriptor) -> false
            isRefAttr(elementDescriptor) -> true
            else -> throw QBitException("$elementKind not yet supported")
        }

        return this
    }

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        val element = elements[index]
        return if(!isRefList) {
            element as T?
        } else {
            decodeReferred(element as Gid, deserializer) as T?
        }
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T {
        return decodeNullableSerializableElement(descriptor, index, deserializer as DeserializationStrategy<Any?>) as T
    }

    private fun isValueAttr(elementDescriptor: SerialDescriptor): Boolean {
        val listElementsKind = elementDescriptor.kind
        return listElementsKind is PrimitiveKind ||
                listElementsKind is StructureKind.LIST // ByteArrays
    }

    private fun isRefAttr(elementDescriptor: SerialDescriptor): Boolean {
        return elementDescriptor.kind is StructureKind.CLASS
    }

    private fun decodeReferred(gid: Gid, deserializer: DeserializationStrategy<*>): Any? {
        val referee = entity.pull(gid) ?: throw QBitException("Dangling ref: $gid")
        val decoder = EntityDecoder(schema, referee, serializersModule)
        return cache.getOrPut(gid) { deserializer.deserialize(decoder) }
    }

    private fun <T : Any> decodeElement(descriptor: SerialDescriptor, index: Int): T {
        val attrName = AttrName(descriptor, index).asString()
        return entity[schema(attrName) as Attr<T>]
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (indexCounter < elements.size) indexCounter++ else CompositeDecoder.DECODE_DONE
    }

    @ExperimentalSerializationApi
    override fun decodeInline(inlineDescriptor: SerialDescriptor): Decoder {
        return this
    }

    @ExperimentalSerializationApi
    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
        return this
    }

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return decodeElement(descriptor, index)
    }

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        return decodeElement(descriptor, index)
    }

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        return decodeElement(descriptor, index)
    }

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        return decodeElement(descriptor, index)
    }

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        return decodeElement(descriptor, index)
    }

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
        return decodeElement(descriptor, index)
    }

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        return decodeElement(descriptor, index)
    }

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        return decodeElement(descriptor, index)
    }
}