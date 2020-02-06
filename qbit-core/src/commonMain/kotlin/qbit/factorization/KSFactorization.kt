package qbit.factorization

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.SerialModuleCollector
import kotlinx.serialization.modules.getContextual
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.AttrValue
import qbit.api.model.Eav
import qbit.api.model.eq
import qbit.collections.IdentityMap
import kotlin.reflect.KClass

class ToStrSerialModuleCollector : SerialModuleCollector {

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

}

fun SerialModule.dump(): String {
    val collector = ToStrSerialModuleCollector()
    this.dumpTo(collector)
    return collector.buffer.toString()
}

class KSFactorization(private val serialModule: SerialModule) {

    fun ksDestruct(e: Any, schema: (String) -> Attr<*>?, gids: Iterator<Gid>): EntityGraphFactorization {
        val encoder = EntityEncoder(schema, serialModule, gids)
        val serializer = serialModule.getContextual(e) ?: throw QBitException("Cannon find serializer for $e (${e::class})\nserializers are available for:\n${serialModule.dump()}")
        serializer.serialize(encoder, e)
        val gid = encoder.gid
        val eavs = encoder.attrValues.map { it.toEav(gid) }.toMutableList()
        for (cEE in encoder.children) {
            val g = cEE.gid
            eavs += cEE.attrValues.map { it.toEav(g) }
        }
        return EntityGraphFactorization(IdentityMap(e to eavs))
    }

}

private fun AttrValue<*, *>.toEav(gid: Gid): Eav =
    Eav(gid, this.attr, this.value)

class EntityEncoder(
    private val schema: (String) -> Attr<*>?,
    override val context: SerialModule,
    private val gids: Iterator<Gid>
) : Encoder,
    CompositeEncoder {

    internal val attrValues = ArrayList<AttrValue<*, *>>()

    internal var gid: Gid = Gid(0)

    internal val children = ArrayList<EntityEncoder>()

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        return this
    }

    override fun endStructure(desc: SerialDescriptor) {
        gid = gids.next()
    }

    override fun encodeBoolean(value: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeByte(value: Byte) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeChar(value: Char) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeDouble(value: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeEnum(enumDescription: SerialDescriptor, ordinal: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeFloat(value: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeInt(value: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeLong(value: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeNotNullMark() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeNull() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeShort(value: Short) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeString(value: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeUnit() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) {
        attrValues += schema(attrName(desc, index))!! eq value
    }

    override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) {
        attrValues += schema(attrName(desc, index))!! eq value
    }

    override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) {
        attrValues += schema(attrName(desc, index))!! eq value
    }

    override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) {
        attrValues += schema(attrName(desc, index))!! eq value
    }

    override fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> encodeNullableSerializableElement(
        desc: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value != null) {
            if (value is Long && desc.getElementName(index) == "id") {
                gid = Gid(value)
            } else {
                attrValues += schema(attrName(desc, index))!! eq value
            }
        }
    }

    override fun <T> encodeSerializableElement(
        desc: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        children.add(EntityEncoder(schema, context, gids))
        serializer.serialize(children.last(), value)
        attrValues += schema(attrName(desc, index))!! eq children.last().gid
    }

    override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
        val attrName = attrName(desc, index)
        val attr = schema(attrName) ?: throw QBitException("Could not find attribute with name $attrName")
        attrValues += attr eq value
    }

    override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) {
        throw QBitException("qbit does not support Float data type")
    }

    override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) {
        throw QBitException("qbit does not support Short data type")
    }

    override fun encodeUnitElement(desc: SerialDescriptor, index: Int) {
        throw QBitException("qbit does not support Unit data type")
    }

    override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) {
        throw QBitException("qbit does not support Char data type")
    }

    override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) {
        throw QBitException("qbit does not support Double data type")
    }


}

internal fun attrName(desc: SerialDescriptor, index: Int) =
    ".${desc.name}/${desc.getElementName(index)}"

