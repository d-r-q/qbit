package qbit.factorization

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.getContextual
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.AttrValue
import qbit.api.model.Eav
import qbit.api.model.eq
import qbit.collections.IdentityMap

class KSFactorization(private val serialModule: SerialModule) {

    fun ksDestruct(e: Any, schema: (String) -> Attr<*>?, gids: Iterator<Gid>): EntityGraphFactorization {
        val encoder = EntityEncoder(schema, serialModule, gids)
        serialModule.getContextual(e)!!.serialize(encoder, e)
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        attrValues += schema(attrName(desc, index))!! eq value
    }

    private fun attrName(desc: SerialDescriptor, index: Int) =
        ".${desc.name}/${desc.getElementName(index)}"

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
