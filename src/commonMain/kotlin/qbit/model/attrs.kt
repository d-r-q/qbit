package qbit.model

import qbit.*
import qbit.ns.Key
import qbit.ns.Namespace

// Interface


sealed class Attr<out T : Any> : RoEntity<EID?> {

    abstract val name: Key

    abstract val type: DataType<T>

    abstract val unique: Boolean

    fun isList() = type.isList()

    fun str() = name.toStr()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this::class != other?.let { it::class }) return false

        other as Attr<*>

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}

sealed class ValAttr<out T : Any> : Attr<T>()

sealed class ScalarAttr<out T : Any> : ValAttr<T>()

sealed class ListAttr<out T : Any> : ValAttr<List<T>>()

sealed class RefAttr<out T : Any> : Attr<T>()

sealed class ScalarRefAttr : RefAttr<RoEntity<EID?>>()

sealed class RefListAttr : RefAttr<List<RoEntity<EID?>>>()


fun <T : Any> ScalarAttr(name: Key, type: DataType<T>, unique: Boolean = false): ScalarAttr<T> = ScalarAttrImpl(name, type, unique)

fun RefAttr(name: Key, unique: Boolean = false): ScalarRefAttr = ScalarRefAttrImpl(name, QRef, unique)

fun <T : Any> ListAttr(name: Key, type: DataType<T>, unique: Boolean = false): ListAttr<T> = ListAttrImpl(name, type, unique)

fun RefListAttr(name: Key, unique: Boolean = false): RefListAttr = RefListAttrImpl(name, unique)

// Utilities


@Suppress("UNCHECKED_CAST")
infix fun <T : Any> Attr<T>.eq(v: T): AttrValue<Attr<T>, T> = when (this) {
    is ScalarAttr -> this eq v
    is ScalarRefAttr -> (this eq (v as RoEntity<*>)) as AttrValue<Attr<T>, T>
    is ListAttr<*> -> (this eq (v as List<T>)) as AttrValue<Attr<T>, T>
    is RefListAttr -> (this eq (v as List<RoEntity<*>>)) as AttrValue<Attr<T>, T>
}

infix fun <T : Any> ScalarAttr<T>.eq(v: T): ScalarAttrValue<T> = ScalarAttrValue(this, v)

infix fun <T : Any> ListAttr<T>.eq(v: List<T>): AttrValue<Attr<List<T>>, List<T>> = ListAttrValue(this, v)

infix fun ScalarRefAttr.eq(v: RoEntity<*>): ScalarRefAttrValue = ScalarRefAttrValue(this, v)

infix fun RefListAttr.eq(v: List<RoEntity<*>>): RefListAttrValue = RefListAttrValue(this, v)


// Implementation


internal fun ListAttr(name: String, type: DataType<*>, unique: Boolean = false): Attr<*> = ListAttr(Key(name), type, unique)

internal fun Attr(name: String, type: DataType<*>, unique: Boolean = false): Attr<*> = ScalarAttr(Key(name), type, unique)

internal fun RefAttr(name: String, unique: Boolean = false): Attr<*> = RefAttr(Key(name), unique)

internal fun RefListAttr(name: String, unique: Boolean = false): Attr<*> = RefListAttr(Key(name), unique)

private data class ScalarAttrImpl<T : Any>(override val name: Key, override val type: DataType<T>, override val unique: Boolean = false) : ScalarAttr<T>(), RoEntity<EID?> by AttrEntityImpl(name, type, unique)

private data class ScalarRefAttrImpl(override val name: Key, override val type: DataType<RoEntity<*>>, override val unique: Boolean = false) : ScalarRefAttr(), RoEntity<EID?> by AttrEntityImpl(name, type, unique)

// TODO: what is unique means for lists?
private data class ListAttrImpl<T : Any>(override val name: Key, val itemsType: DataType<T>,
                                         override val unique: Boolean = false)
    :
        ListAttr<T>(), RoEntity<EID?> by AttrEntityImpl(name, itemsType, unique, true) {

    override val type: DataType<List<T>> = this.itemsType.list()

}

private data class RefListAttrImpl(override val name: Key, override val unique: Boolean = false)
    :
        RefListAttr(), RoEntity<EID?> by AttrEntityImpl(name, QRef, unique, true) {

    override val type: DataType<List<RoEntity<*>>> = QRef.list()

}

private data class AttrEntityImpl(val name: Key, val type: DataType<*>, val unique: Boolean = false, val list: Boolean = false) : RoEntity<EID?>{

    override val eid: EID? = null

    private val map = mapOf(Attrs.name to name.toStr(), Attrs.type to type.code, Attrs.unique to unique, Attrs.list to list)

    override val keys: Set<ScalarAttr<Any>> by lazy {  map.keys.toSet() }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> tryGet(key: Attr<T>): T? = (map as Map<Attr<T>, T>)[key]

}

private const val nsSep = "."
private const val keySep = "/"

private fun Key.toStr() = this.ns.parts.joinToString(nsSep) + keySep + this.name

private fun Key(keyStr: String): Key {
    val parts = keyStr.split(keySep)
    if (parts.size != 2) {
        throw IllegalArgumentException("Malformed attribute name: $keyStr")
    }
    val (ns, name) = parts
    return Namespace.of(*ns.split(nsSep).toTypedArray())[name]
}
