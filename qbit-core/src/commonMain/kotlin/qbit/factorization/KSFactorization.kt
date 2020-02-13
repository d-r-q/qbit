package qbit.factorization

import kotlinx.serialization.*
import kotlinx.serialization.PrimitiveKind.*
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.SerialModuleCollector
import kotlinx.serialization.modules.getContextual
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.gid.NullGid
import qbit.api.model.Attr
import qbit.api.model.AttrValue
import qbit.api.model.Eav
import qbit.api.model.eq
import qbit.api.model.impl.QbitAttrValue
import qbit.collections.IdentityMap
import qbit.collections.Stack
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
        val encoder = EntityEncoder(e, schema, serialModule, gids)
        val serializer = serialModule.getContextual(e)
            ?: throw QBitException("Cannon find serializer for $e (${e::class})\nserializers are available for:\n${serialModule.dump()}")
        serializer.serialize(encoder, e)
        val eavs: List<Pair<Any, List<Eav>>> = encoder.entityInfos.map { it.key to it.value.eavs() }.toList()
        return EntityGraphFactorization(IdentityMap(*eavs.toTypedArray()))
    }

}

private fun AttrValue<*, *>.toEav(gid: Gid): Eav =
    Eav(gid, this.attr, this.value)

data class EntityInfo(
    val entity: Any,
    val attr: Attr<*>?,
    var gid: Gid = NullGid,
    val attrValues: ArrayList<AttrValue<Attr<*>, *>> = ArrayList(),
    val type: StructureKind
) {

    fun eavs(): List<Eav> =
        attrValues.map { it.toEav(gid) }
}

class EntityEncoder(
    internal val entity: Any,
    private val schema: (String) -> Attr<*>?,
    override val context: SerialModule,
    private val gids: Iterator<Gid>
) : Encoder,
    CompositeEncoder {

    private val structuresStack = Stack<EntityInfo>().apply {
        push(EntityInfo(entity, null, type = StructureKind.CLASS))
    }

    internal val entityInfos = IdentityMap<Any, EntityInfo>()

    internal var gid: Gid = Gid(0)

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        println("beginStructure: $desc")
        return this
    }

    override fun endStructure(desc: SerialDescriptor) {
        println("endStructure: $desc")
        val ei = structuresStack.peek()
        if (ei.type == StructureKind.CLASS && ei.gid == Gid(0)) {
            ei.gid = gids.next()
        }

        if (structuresStack.size == 1) {
            // end of graph root
            entityInfos[entity] = structuresStack.pop()
        }
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
        addAttrValue(AttrValue(desc, index, value))
    }

    override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) {
        addAttrValue(AttrValue(desc, index, value))
    }

    override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) {
        println("encodeIntElement: $desc $index $value")
        addAttrValue(AttrValue(desc, index, value))
    }

    override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) {
        addAttrValue(AttrValue(desc, index, value))
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
                addAttrValue(AttrValue(desc, index, value))
            }
        }
    }

    override fun <T> encodeSerializableElement(
        desc: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        val elementDescriptor = desc.getElementDescriptor(index)
        println("encodeSerializableElement: $elementDescriptor")
        if (value == null) {
            return
        }

        when (elementDescriptor.kind) {
            StructureKind.CLASS -> {
                val attr = when (structuresStack.peek().type) {
                    StructureKind.LIST -> structuresStack.peek().attr!!
                    else -> Attr(desc, index)
                }
                structuresStack.push(EntityInfo(value, attr, type = StructureKind.CLASS))
                entityInfos[structuresStack.peek().entity] = structuresStack.peek()
                serializer.serialize(this, value)
                val entityInfo =
                    entityInfos.get(value as Any) ?: throw QBitException("Entity info not found after serialization")
                structuresStack.pop()
                addAttrValue(attr eq entityInfo.gid)
            }
            StructureKind.LIST -> {
                structuresStack.push(EntityInfo(value, Attr(desc, index), type = StructureKind.LIST))
                serializer.serialize(this, value)
                val listAttrVals = structuresStack.pop()
                structuresStack.peek().attrValues.addAll(listAttrVals.attrValues)
            }
            INT, UNIT, BOOLEAN, BYTE, SHORT, LONG, FLOAT, DOUBLE, CHAR, STRING -> {
                structuresStack.peek().attrValues.add(QbitAttrValue<Any>(structuresStack.peek().attr!!, value))
            }
            else -> throw QBitException("Serialization of $elementDescriptor isn't supported")
        }
    }

    override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
        println("encodeStringElement: $desc, $index, $value")
        addAttrValue(AttrValue(desc, index, value))
    }

    private fun addAttrValue(attrVal: AttrValue<Attr<*>, *>) {
        structuresStack.peek().attrValues.add(attrVal)
    }

    private fun AttrValue(desc: SerialDescriptor, index: Int, value: Any): AttrValue<Attr<Any>, Any> {
        val attr = Attr(desc, index)
        return attr eq value
    }

    private fun Attr(desc: SerialDescriptor, index: Int): Attr<*> {
        val attrName = attrName(desc, index)
        return schema(attrName) ?: throw QBitException("Could not find attribute with name $attrName")
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

