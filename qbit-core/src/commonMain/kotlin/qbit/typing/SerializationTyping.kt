package qbit.typing

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.DataType
import qbit.api.model.StoredEntity
import qbit.factoring.serializatoin.AttrName
import qbit.index.IndexedRegister
import kotlin.reflect.KClass

fun <T : Any> typify(
    schema: (String) -> Attr<*>?,
    entity: StoredEntity,
    type: KClass<T>,
    serialModule: SerializersModule,
    registerFolders: Map<String, (Any, Any) -> Any>
): T {
    val contextual = serialModule.getContextual(type) ?: throw QBitException("Cannot find serializer for $type")
    return contextual.deserialize(EntityDecoder(schema, entity, serialModule, registerFolders))
}

@Suppress("UNCHECKED_CAST")
class EntityDecoder(
    val schema: (String) -> Attr<*>?,
    val entity: StoredEntity,
    override val serializersModule: SerializersModule,
    val registerFolders: Map<String, (Any, Any) -> Any>
) : StubDecoder() {

    private var fields = 0

    private var idx = 0

    private val cache = HashMap<Gid, Any?>()

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        fields = descriptor.elementsCount
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

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        val elementDescriptor = descriptor.getElementDescriptor(index)
        val elementKind = elementDescriptor.kind

        if (descriptor.getElementName(index) == "id") {
            return when (elementKind) {
                PrimitiveKind.LONG -> entity.gid.value() as T?
                StructureKind.CLASS -> entity.gid as T?
                else -> throw QBitException("Corrupted entity, id field has kind $elementKind")
            }
        }

        val attrName = AttrName(descriptor, index).asString()
        val attr: Attr<T> = schema(attrName) as Attr<T>?
            ?: throw QBitException("Corrupted entity $entity, there is no attr $attrName in schema")
        val dataType = DataType.ofCode(attr.type)!!

        if(dataType.isList()) {
            val elements = entity.tryGet(attr) ?: return null // TODO CHECK NULLABILITY
            val decoder = ListDecoder(schema, entity, elements as List<Any>, serializersModule, cache, registerFolders)
            return deserializer.deserialize(decoder)
        }

        if(dataType.isRegister()) {
            val register = entity.tryGet(attr) as IndexedRegister? ?: return null
            val folder = registerFolders[attrName] ?: throw QBitException("There is no folder for attr $attrName")
            val values = when {
                isValueAttr(elementDescriptor) -> register.values()
                isRefAttr(elementDescriptor) -> register.values().map {
                    decodeReferred(
                        elementDescriptor,
                        attrName,
                        it as Gid,
                        deserializer
                    ) as Any // TODO NULLABILITY
                }
                else -> throw QBitException("$elementKind not yet supported")
            }
            return values.reduce(folder) as T?
        }

        return when {
            isValueAttr(elementDescriptor) -> entity.tryGet(attr)
            isRefAttr(elementDescriptor) -> decodeReferred(
                elementDescriptor,
                attrName,
                entity.tryGet(attr as Attr<Gid>),
                deserializer
            ) as T?
            else -> throw QBitException("$elementKind not yet supported")
        }
    }

    private fun <T : Any> decodeReferred(
        elementDescriptor: SerialDescriptor,
        attrName: String,
        gid: Gid?,
        deserializer: DeserializationStrategy<T?>,
    ): Any? {
        when {
            gid == null && elementDescriptor.isNullable -> return null
            gid == null && !elementDescriptor.isNullable -> throw QBitException("Corrupted entity: $entity, no value for $attrName")
        }
        check(gid != null)

        val referee = entity.pull(gid) ?: throw QBitException("Dangling ref: $gid")
        val decoder = EntityDecoder(schema, referee, serializersModule, registerFolders)
        return cache.getOrPut(gid) { deserializer.deserialize(decoder) }
    }

    private fun isValueAttr(elementDescriptor: SerialDescriptor): Boolean {
        val elementKind = elementDescriptor.kind
        val listElementsKind = elementDescriptor.takeIf { it.kind is StructureKind.LIST }?.getElementDescriptor(0)?.kind
        return elementKind is PrimitiveKind || listElementsKind is PrimitiveKind
    }

    private fun isRefAttr(elementDescriptor: SerialDescriptor): Boolean {
        return elementDescriptor.kind is StructureKind.CLASS
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T {
        return decodeNullableSerializableElement(descriptor, index, deserializer as DeserializationStrategy<Any?>) as T
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (idx < fields) idx++ else DECODE_DONE
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

    private fun <T : Any> decodeElement(descriptor: SerialDescriptor, index: Int): T {
        val attrName = AttrName(descriptor, index).asString()
        val element = entity[schema(attrName) as Attr<T>]
        return if (element is IndexedRegister) {
            val folder = registerFolders[attrName] ?: throw QBitException("There is no folder for attr $attrName")
            element.values().reduce(folder) as T
        } else {
            element
        }
    }

}