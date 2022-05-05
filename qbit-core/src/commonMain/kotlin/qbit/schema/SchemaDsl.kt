package qbit.schema

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.SerializersModule
import qbit.api.QBitException
import qbit.api.model.*
import qbit.factoring.serializatoin.AttrName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

fun schema(serialModule: SerializersModule, body: SchemaBuilder.() -> Unit): Pair<List<Attr<Any>>, HashMap<String, (Any, Any) -> Any>> {
    val scb = SchemaBuilder(serialModule)
    scb.body()
    return scb.attrs to scb.folders
}

class SchemaBuilder(private val serialModule: SerializersModule) {

    internal val attrs: MutableList<Attr<Any>> = ArrayList()

    internal val folders: HashMap<String, (Any, Any) -> Any> = HashMap()

    fun <T : Any> entity(type: KClass<T>, body: EntityBuilder<T>.() -> Unit = {}) {
        val descr =
            serialModule.getContextual(type)?.descriptor
                ?: throw QBitException("Cannot find descriptor for $type")
        val eb = EntityBuilder<T>(descr)
        eb.body()
        folders += eb.registerFolders
        attrs.addAll(schemaFor(descr, eb.uniqueProps, eb.counters, eb.registerFolders.keys))
    }

}

class EntityBuilder<T : Any>(private val descr: SerialDescriptor) {

    internal val uniqueProps = HashSet<String>()

    internal val counters = HashSet<String>()

    internal val registerFolders = HashMap<String, (Any, Any) -> Any>()

    fun uniqueInt(prop: KProperty1<T, Int>) {
        uniqueAttr(prop)
    }

    fun uniqueString(prop: KProperty1<T, String>) {
        uniqueAttr(prop)
    }

    private fun uniqueAttr(prop: KProperty1<T, *>) {
        uniqueProps.add(getAttrName(prop))
    }

    fun byteCounter(prop: KProperty1<T, Byte>) {
        counter(prop)
    }

    fun intCounter(prop: KProperty1<T, Int>) {
        counter(prop)
    }

    fun longCounter(prop: KProperty1<T, Long>) {
        counter(prop)
    }

    private fun counter(prop: KProperty1<T, *>) {
        counters.add(getAttrName(prop))
    }

    fun<V> register(prop: KProperty1<T, V>, fold: (V, V) -> V) {
        registerFolders.put(getAttrName(prop), fold as (Any, Any) -> Any)
    }

    private fun getAttrName(prop: KProperty1<T, *>): String {
        val (idx, _) = descr.elementNames
            .withIndex().firstOrNull { (_, name) -> name == prop.name }
            ?: throw QBitException("Cannot find attr for ${prop.name} in $descr")
        return AttrName(descr, idx).asString()
    }

}

fun schemaFor(rootDesc: SerialDescriptor, unique: Set<String> = emptySet(), counters: Set<String> = emptySet(), registers: Set<String> = emptySet()): List<Attr<Any>> {
    return rootDesc.elementDescriptors
        .withIndex()
        .filter { rootDesc.getElementName(it.index) !in setOf("id", "gid") }
        .map { (idx, desc) ->
            val attr = AttrName(rootDesc, idx).asString()
            val dataType = when {
                attr in counters -> DataType.of(desc).counter()
                attr in registers -> DataType.of(desc).register()
                else -> DataType.of(desc)
            }
            Attr<Any>(null, attr, dataType.code, attr in unique, dataType.isList())
        }
}

private fun DataType.Companion.of(desc: SerialDescriptor): DataType<*> =
    when (desc.kind) {
        StructureKind.CLASS -> QRef
        StructureKind.LIST -> {
            val listElementDesc = desc.getElementDescriptor(0)
            when (listElementDesc.kind) {
                PrimitiveKind.BYTE -> {
                    when (desc.serialName) {
                        "kotlin.ByteArray", "kotlin.ByteArray?" -> QBytes
                        "kotlin.collections.ArrayList", "kotlin.collections.ArrayList?" -> QByte.list()
                        else -> throw AssertionError("Unexpected descriptor: ${desc.serialName}")
                    }
                }
                StructureKind.LIST -> QBytes.list()
                else -> DataType.of(listElementDesc).list()
            }
        }
        PrimitiveKind.STRING -> QString
        PrimitiveKind.BOOLEAN -> QBoolean
        PrimitiveKind.BYTE -> QByte
        PrimitiveKind.INT -> QInt
        PrimitiveKind.LONG -> QLong
        else -> throw QBitException("Unsupported SerialKind ${desc.kind}")
    }
