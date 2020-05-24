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
    serialModule: SerialModule
): T {
    val contextual = serialModule.getContextual(type) ?: throw QBitException("Cannot find serializer for $type")
    return contextual.deserialize(EntityDecoder(schema, entity, serialModule))
}

class EntityDecoder(
    val schema: (String) -> Attr<*>?,
    val entity: StoredEntity,
    override val context: SerialModule,
    override val updateMode: UpdateMode = UpdateMode.BANNED
) : Decoder, CompositeDecoder {

    private var fields = 0

    private var idx = 0

    private val cache = HashMap<Gid, Any?>()

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        fields = descriptor.elementsCount
        return this
    }

    override fun decodeBoolean(): Boolean {
        TODO("Not yet implemented")
    }

    override fun decodeByte(): Byte {
        TODO("Not yet implemented")
    }

    override fun decodeChar(): Char {
        TODO("Not yet implemented")
    }

    override fun decodeDouble(): Double {
        TODO("Not yet implemented")
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        TODO("Not yet implemented")
    }

    override fun decodeFloat(): Float {
        TODO("Not yet implemented")
    }

    override fun decodeInt(): Int {
        TODO("Not yet implemented")
    }

    override fun decodeLong(): Long {
        TODO("Not yet implemented")
    }

    override fun decodeNotNullMark(): Boolean {
        TODO("Not yet implemented")
    }

    override fun decodeNull(): Nothing? {
        TODO("Not yet implemented")
    }

    override fun decodeShort(): Short {
        TODO("Not yet implemented")
    }

    override fun decodeString(): String {
        TODO("Not yet implemented")
    }

    override fun decodeUnit() {
        TODO("Not yet implemented")
    }

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return decodeElement(descriptor, index)
    }

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        return decodeElement(descriptor, index)
    }

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        TODO("Not yet implemented")
    }

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        TODO("Not yet implemented")
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        println(descriptor.serialName)
        return if (idx < fields) idx++ else READ_DONE
    }

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        TODO("Not yet implemented")
    }

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
        return decodeElement(descriptor, index)
    }

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        return decodeElement(descriptor, index)
    }


    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>
    ): T? {
        println(descriptor.serialName)
        println(descriptor.getElementName(index))
        if (descriptor.getElementName(index) == "id") {
            return when (val kind = descriptor.getElementDescriptor(index).kind) {
                PrimitiveKind.LONG -> entity.gid.value() as T?
                StructureKind.CLASS -> entity.gid as T?
                else -> throw QBitException("Corrupted entity, id field has kind $kind")
            }
        }
        if (descriptor.kind == StructureKind.LIST && descriptor.getElementDescriptor(index).kind == StructureKind.CLASS) {
            val decoder = EntityDecoder(schema, entity, context)
            return deserializer.deserialize(decoder)
        }
        val attrName = AttrName(descriptor, index).asString()
        val attr: Attr<T> = schema(attrName) as Attr<T>?
            ?: throw QBitException("Corrupted entity $entity, there is no attr $attrName in schema")
        return when (val kind = descriptor.getElementDescriptor(index).kind) {
            is PrimitiveKind -> entity.tryGet(attr)
            is StructureKind.CLASS -> {
                val gid = entity.tryGet(attr as Attr<Gid>)
                val referree = gid?.let { entity.pull(it) }
                if (referree != null) {
                    val decoder = EntityDecoder(schema, referree, context)
                    cache.getOrPut(gid, { deserializer.deserialize(decoder) }) as T?
                } else {
                    if (descriptor.getElementDescriptor(index).isNullable) {
                        return null
                    } else {
                        throw QBitException("Corrupted entity: $entity, no value for $attrName")
                    }
                }
            }
            is StructureKind.LIST -> {
                when (val elementsKind = descriptor.getElementDescriptor(index).getElementDescriptor(0).kind) {
                    is PrimitiveKind.BYTE -> entity.tryGet(attr) ?: null as T?
                    is PrimitiveKind -> entity.tryGet(attr) ?: null as T?
                    is StructureKind.CLASS -> {
                        val gids = entity.tryGet(attr) as List<Gid>?
                        if (gids == null) {
                            null
                        } else {
                            gids.map {
                                val decoder = EntityDecoder(
                                    schema,
                                    entity.pull(it) ?: throw QBitException("Dangling ref: $it"),
                                    context
                                )
                                val res = cache.getOrPut(it, { deserializer.deserialize(decoder) })
                                if (res is List<*>) {
                                    res[0]
                                } else {
                                    res
                                }
                            } as T?
                        }
                    }
                    is StructureKind.LIST -> {
                        entity.tryGet(attr) ?: null as T?
                    }
                    else -> throw TODO("$elementsKind not yet supported")
                }
            }
            else -> throw TODO("$kind not yet supported")
        }
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>
    ): T {
        deserializer as DeserializationStrategy<Any>
        return tmp(descriptor, index, deserializer) as T
    }

    private fun <T : Any> tmp(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>
    ): T? {
        val nullableDeserializer: DeserializationStrategy<T?> = deserializer as DeserializationStrategy<T?>
        val nonNullResult = decodeNullableSerializableElement(descriptor, index, nullableDeserializer)
        return nonNullResult
    }

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
        TODO("Not yet implemented")
    }

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        println(descriptor.getElementName(index))
        return decodeElement(descriptor, index)
    }

    private fun <T : Any> decodeElement(descriptor: SerialDescriptor, index: Int): T {
        val attrName = AttrName(descriptor, index).asString()
        return entity[schema(attrName) as Attr<T>]
    }


    override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int) {
        TODO("Not yet implemented")
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }

    override fun <T : Any> updateNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        old: T?
    ): T? {
        println("updateNullableSerializableElement: $old")
        return old
    }

    override fun <T> updateSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        old: T
    ): T {
        TODO("Not yet implemented")
    }

}