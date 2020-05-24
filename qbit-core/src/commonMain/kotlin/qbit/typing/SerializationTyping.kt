package qbit.typing

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.modules.SerialModule
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.StoredEntity
import qbit.factoring.serializatoin.AttrName
import kotlin.reflect.KClass

fun <T : Any> typify(
    schema: (String) -> Attr<*>?,
    entity: StoredEntity,
    type: KClass<T>,
    serialModule: SerialModule,
): T {
    val contextual = serialModule.getContextual(type) ?: throw QBitException("Cannot find serializer for $type")
    return contextual.deserialize(EntityDecoder(schema, entity, serialModule))
}

@Suppress("UNCHECKED_CAST")
class EntityDecoder(
    val schema: (String) -> Attr<*>?,
    val entity: StoredEntity,
    override val context: SerialModule,
    override val updateMode: UpdateMode = UpdateMode.BANNED,
) : StubDecoder() {

    private var fields = 0

    private var idx = 0

    private val cache = HashMap<Gid, Any?>()

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        fields = descriptor.elementsCount
        return this
    }

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>
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

        if (descriptor.kind == StructureKind.LIST && elementKind == StructureKind.CLASS) {
            val decoder = EntityDecoder(schema, entity, context)
            return deserializer.deserialize(decoder)
        }

        val attrName = AttrName(descriptor, index).asString()
        val attr: Attr<T> = schema(attrName) as Attr<T>?
            ?: throw QBitException("Corrupted entity $entity, there is no attr $attrName in schema")

        return when {
            isValueAttr(elementDescriptor) -> entity.tryGet(attr)
            isRefAttr(elementDescriptor) -> decodeReferred(elementDescriptor, attrName, entity.tryGet(attr as Attr<Gid>), deserializer) as T?
            else -> throw QBitException("$elementKind not yet supported")
        }
    }

    private fun <T : Any> decodeReferred(
        elementDescriptor: SerialDescriptor,
        attrName: String,
        gids: Any?,
        deserializer: DeserializationStrategy<T?>,
    ): Any? {
        when {
            gids == null && elementDescriptor.isNullable -> return null
            gids == null && !elementDescriptor.isNullable -> throw QBitException("Corrupted entity: $entity, no value for $attrName")
        }
        check(gids != null)

        val sureGids = when (gids) {
            is Gid -> listOf(gids)
            is List<*> -> gids as List<Gid>
            else -> throw AssertionError("Unexpected gids: $gids")
        }

        val referreds = sureGids.map {
            val referee = entity.pull(it) ?: throw QBitException("Dangling ref: $it")
            val decoder = EntityDecoder(schema, referee, context)
            val res = cache.getOrPut(it, { deserializer.deserialize(decoder) })
            if (res is List<*>) {
                res[0] as T
            } else {
                res as T
            }
        }

        return when (elementDescriptor.kind) {
            is StructureKind.CLASS -> referreds[0]
            is StructureKind.LIST -> referreds
            else -> throw AssertionError("Unexpected kind: ${elementDescriptor.kind}")
        }
    }

    private fun isValueAttr(elementDescriptor: SerialDescriptor): Boolean {
        val elementKind = elementDescriptor.kind
        val listElementsKind = elementDescriptor.takeIf { it.kind is StructureKind.LIST }?.getElementDescriptor(0)?.kind
        return elementKind is PrimitiveKind || listElementsKind is PrimitiveKind ||
                listElementsKind is StructureKind.LIST // List of ByteArrays
    }

    private fun isRefAttr(elementDescriptor: SerialDescriptor): Boolean {
        val elementKind = elementDescriptor.kind
        val listElementsKind = elementDescriptor.takeIf { it.kind is StructureKind.LIST }?.getElementDescriptor(0)?.kind
        return elementKind is StructureKind.CLASS || listElementsKind is StructureKind.CLASS
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
    ): T {
        return decodeNullableSerializableElement(descriptor, index, deserializer as DeserializationStrategy<Any?>) as T
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (idx < fields) idx++ else READ_DONE
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
        return entity[schema(attrName) as Attr<T>]
    }

}