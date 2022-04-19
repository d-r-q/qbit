package qbit.typing

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.StoredEntity
import qbit.factoring.serializatoin.AttrName

@Suppress("UNCHECKED_CAST")
class RegisterDecoder(
    val schema: (String) -> Attr<*>?,
    val entity: StoredEntity,
    private val elements: List<Any>,
    override val serializersModule: SerializersModule,
    private val cache: HashMap<Gid, Any?>
) : StubDecoder() {
    private var listDecoded = false

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        val decoder = ListDecoder(schema, entity, elements, serializersModule, cache)
        return deserializer.deserialize(decoder)
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T {
        return decodeNullableSerializableElement(descriptor, index, deserializer as DeserializationStrategy<Any?>) as T
    }

    private fun <T : Any> decodeElement(descriptor: SerialDescriptor, index: Int): T {
        val attrName = AttrName(descriptor, index).asString()
        return entity[schema(attrName) as Attr<T>]
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if(listDecoded) {
            return CompositeDecoder.DECODE_DONE
        } else {
            listDecoded = true
            return 0
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return this
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