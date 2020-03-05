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

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeSerializers: KSerializer<*>): CompositeEncoder {
        validateEntity(descriptor)
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
                "List of nullable elements is not supported. Properties: ${desc.serialName}.${nullableListProps.map { it }.joinToString(
                    ",",
                    "(",
                    ")"
                )}"
            )
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
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

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
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

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        addAttrValue(AttrValue(descriptor, index, value))
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        addAttrValue(AttrValue(descriptor, index, value))
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        println("encodeIntElement: $descriptor $index $value")
        addAttrValue(AttrValue(descriptor, index, value))
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        addAttrValue(AttrValue(descriptor, index, value))
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        println("encodeNullableSerializableElement: $descriptor $index $value")
        if (value != null) {
            encodeSerializableElement(descriptor, index, serializer, value)
        }
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        // workaround for: https://github.com/Kotlin/kotlinx.serialization/issues/739
        val di = if (descriptor.kind == StructureKind.LIST) 0 else index
        val elementDescriptor = descriptor.getElementDescriptor(di)
        //println("encodeSerializableElement: $elementDescriptor")
        if (value == null) {
            return
        }

        tryToTreatAsGid(value, descriptor.getElementName(di))?.let { gid ->
            structuresStack.peek().gid = gid
            if ("QTombstone" in descriptor.serialName) {
                addAttrValue(QbitAttrValue(tombstone, true))
            }
            return
        }

        when (elementDescriptor.kind) {
            StructureKind.CLASS -> {
                val attr = when (structuresStack.peek().type) {
                    StructureKind.LIST -> structuresStack.peek().attr!!
                    else -> Attr(descriptor, di)
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
                    structuresStack.push(EntityInfo(value, Attr(descriptor, di), type = StructureKind.LIST))
                    serializer.serialize(this, value)
                    val listAttrVals = structuresStack.pop()
                    structuresStack.peek().attrValues.addAll(listAttrVals.attrValues)
                } else if (value is ByteArray) {
                    if (structuresStack.peek().type == StructureKind.LIST) {
                        structuresStack.peek().attrValues.add(QbitAttrValue(structuresStack.peek().attr!!, value))
                    } else {
                        structuresStack.peek().attrValues.add(AttrValue(descriptor, di, value))
                    }
                }
            }
            INT, BOOLEAN, BYTE, SHORT, LONG, FLOAT, DOUBLE, CHAR, STRING -> {
                if (structuresStack.peek().type == StructureKind.LIST) {
                    structuresStack.peek().attrValues.add(QbitAttrValue<Any>(structuresStack.peek().attr!!, value))
                } else {
                    structuresStack.peek().attrValues.add(AttrValue(descriptor, di, value))
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

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        println("encodeStringElement: $descriptor, $index, $value")
        addAttrValue(AttrValue(descriptor, index, value))
    }

    private fun addAttrValue(attrVal: AttrValue<Attr<*>, *>) {
        structuresStack.peek().attrValues.add(attrVal)
    }

    private fun AttrValue(descriptor: SerialDescriptor, index: Int, value: Any): AttrValue<Attr<Any>, Any> {
        val attr = Attr(descriptor, index)
        return attr eq value
    }

    private fun Attr(descriptor: SerialDescriptor, index: Int): Attr<*> {
        val attrName = attrName(descriptor, index)
        return schema(attrName) ?: throw QBitException("Could not find attribute with name $attrName")
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        throw QBitException("qbit does not support Float data type")
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        throw QBitException("qbit does not support Short data type")
    }

    override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) {
        throw QBitException("qbit does not support Unit data type")
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        throw QBitException("qbit does not support Char data type")
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        throw QBitException("qbit does not support Double data type")
    }

}

internal fun attrName(desc: SerialDescriptor, index: Int) =
    ".${desc.serialName}/${desc.getElementName(index)}"

