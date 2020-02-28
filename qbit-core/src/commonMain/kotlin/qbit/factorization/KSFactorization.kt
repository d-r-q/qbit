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
import qbit.api.model.impl.QTombstone
import qbit.api.model.impl.QbitAttrValue
import qbit.api.tombstone
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
            ?: throw QBitException("Cannot find serializer for $e (${e::class})\nserializers are available for:\n${serialModule.dump()}")
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

    private val gidEntityInfos = HashMap<Gid, MutableList<EntityInfo>>()

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        validateEntity(desc)
        //println("beginStructure: $desc")
        return this
    }

    private fun validateEntity(desc: SerialDescriptor) {
        val nullableListProps =
            desc.elementDescriptors()
                .withIndex()
                .map { (idx, eDescr) -> eDescr to desc.getElementName(idx) }
                .filter { (eDescr, _) -> eDescr.kind == StructureKind.LIST && eDescr.getElementDescriptor(0).isNullable }
                .map { it.second }
        if (nullableListProps.isNotEmpty()) {
            throw QBitException(
                "List of nullable elements is not supported. Properties: ${desc.name}.${nullableListProps.map { it }.joinToString(
                    ",",
                    "(",
                    ")"
                )}"
            )
        }
    }

    override fun endStructure(desc: SerialDescriptor) {
        //println("endStructure: $desc")
        val ei = structuresStack.peek()
        if (ei.type == StructureKind.CLASS && ei.gid == NullGid) {
            ei.gid = gids.next()
        }
        if (ei.gid !in gidEntityInfos) {
            gidEntityInfos.getOrPut(ei.gid, { ArrayList() }) += ei
        } else if (ei.type != StructureKind.LIST) {
            val states = gidEntityInfos[ei.gid]?.map { it.entity }
            if (states?.any { it != ei.entity } == true) {
                throw QBitException("Entity ${ei.gid} has several different states to store: ${states + ei.entity}")
            }
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
        println("encodeNullableSerializableElement: $desc $index $value")
        if (value != null) {
            encodeSerializableElement(desc, index, serializer, value)
        }
    }

    override fun <T> encodeSerializableElement(
        desc: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        val elementDescriptor = desc.getElementDescriptor(index)
        //println("encodeSerializableElement: $elementDescriptor")
        if (value == null) {
            return
        }

        tryToTreatAsGid(value, desc.getElementName(index))?.let { gid ->
            structuresStack.peek().gid = gid
            if ("QTombstone" in desc.name) {
                addAttrValue(QbitAttrValue(tombstone, true))
            }
            return
        }

        when (elementDescriptor.kind) {
            StructureKind.CLASS -> {
                val attr = when (structuresStack.peek().type) {
                    StructureKind.LIST -> structuresStack.peek().attr!!
                    else -> Attr(desc, index)
                }
                val entityInfo = if ((value as Any) !in entityInfos) {
                    val entityInfo = EntityInfo(value, attr, type = StructureKind.CLASS)
                    structuresStack.push(entityInfo)
                    serializer.serialize(this, value)
                    entityInfos[value] = gidEntityInfos[entityInfo.gid]?.firstOrNull() ?: throw QBitException("Could not find entity info for gid ${entityInfo.gid}")
                    structuresStack.pop()
                } else {
                    entityInfos[value] ?: throw QBitException("Entity info for $value not found after serialization")
                }
                addAttrValue(attr eq entityInfo.gid)
            }
            StructureKind.LIST -> {
                if (value is List<*>) {
                    structuresStack.push(EntityInfo(value, Attr(desc, index), type = StructureKind.LIST))
                    serializer.serialize(this, value)
                    val listAttrVals = structuresStack.pop()
                    structuresStack.peek().attrValues.addAll(listAttrVals.attrValues)
                } else if (value is ByteArray) {
                    if (structuresStack.peek().type == StructureKind.LIST) {
                        structuresStack.peek().attrValues.add(QbitAttrValue(structuresStack.peek().attr!!, value))
                    } else {
                        structuresStack.peek().attrValues.add(AttrValue(desc, index, value))
                    }
                }
            }
            INT, UNIT, BOOLEAN, BYTE, SHORT, LONG, FLOAT, DOUBLE, CHAR, STRING -> {
                if (structuresStack.peek().type == StructureKind.LIST) {
                    structuresStack.peek().attrValues.add(QbitAttrValue<Any>(structuresStack.peek().attr!!, value))
                } else {
                    structuresStack.peek().attrValues.add(AttrValue(desc, index, value))
                }
            }
            else -> throw QBitException("Serialization of $elementDescriptor isn't supported")
        }
    }

    private fun tryToTreatAsGid(value: Any, name: String): Gid? =
        if ((value is Long || value is Gid) && (name == "id" || name == "gid")) {
            (value as? Gid) ?: Gid((value as Long))
        } else {
            null
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

