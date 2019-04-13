package qbit.schema

import qbit.*
import qbit.ns.Key
import qbit.ns.Namespace

// Interface


interface Attr<T : Any> : Entitiable {

    val name: Key

    val type: DataType<T>

    val unique: Boolean

    fun isList() = type.isList()

    fun str() = name.toStr()

}

interface RefAttr<T : Any> : Attr<T>

interface ScalarRefAttr : RefAttr<Entity>

interface ScalarAttr<T : Any> : Attr<T>

interface ListAttr<T : Any> : Attr<List<T>> {

    val itemsType: DataType<T>

}

interface RefListAttr : RefAttr<List<Entity>>

fun <T : Any> ScalarAttr(name: Key, type: DataType<T>, unique: Boolean = false): ScalarAttr<T> = ScalarAttrImpl(name, type, unique)

fun RefAttr(name: Key, unique: Boolean = false): ScalarRefAttr = ScalarRefAttrImpl(name, QEntity, unique)

fun <T : Any> ListAttr(name: Key, type: DataType<T>, unique: Boolean = false): ListAttr<T> = ListAttrImpl(name, type, unique)

fun RefListAttr(name: Key, unique: Boolean = false): RefListAttr = RefListAttrImpl(name, unique)

// Utilities


infix fun <T : Any> ScalarAttr<T>.eq(v: T): ScalarAttrValue<T> = ScalarAttrValue(this, v)

infix fun <T : Any> ListAttr<T>.eq(v: List<T>): AttrValue<Attr<List<T>>, List<T>> = ListAttrValue(this, v)

infix fun ScalarRefAttr.eq(v: Entity): ScalarRefAttrValue = ScalarRefAttrValue(this, v)

infix fun RefListAttr.eq(v: List<Entity>): RefListAttrValue = RefListAttrValue(this, v)


// Implementation


internal fun ListAttr(name: String, type: DataType<*>, unique: Boolean = false): Attr<*> = ListAttr(Key(name), type, unique)

internal fun Attr(name: String, type: DataType<*>, unique: Boolean = false): Attr<*> = ScalarAttr(Key(name), type, unique)

internal fun RefAttr(name: String, unique: Boolean = false): Attr<*> = RefAttr(Key(name), unique)

internal fun RefListAttr(name: String, unique: Boolean = false): Attr<*> = RefListAttr(Key(name), unique)

private data class ScalarAttrImpl<T : Any>(override val name: Key, override val type: DataType<T>, override val unique: Boolean = false) : ScalarAttr<T>, Entitiable by AttrEntityImpl(name, type, unique)

private data class ScalarRefAttrImpl(override val name: Key, override val type: DataType<Entity>, override val unique: Boolean = false) : ScalarRefAttr, Entitiable by AttrEntityImpl(name, type, unique)

// TODO: what is unique means for lists?
private data class ListAttrImpl<T : Any>(override val name: Key, override val itemsType: DataType<T>,
                                         override val unique: Boolean = false)
    :
        ListAttr<T>, Entitiable by AttrEntityImpl(name, itemsType, unique, true) {

    override val type: DataType<List<T>> = this.itemsType.list()

}

private data class RefListAttrImpl(override val name: Key, override val unique: Boolean = false)
    :
        RefListAttr, Entitiable by AttrEntityImpl(name, QEntity, unique, true) {

    override val type: DataType<List<Entity>> = QEntity.list()

}

private data class AttrEntityImpl(val name: Key, val type: DataType<*>, val unique: Boolean = false, val list: Boolean = false) : Entitiable {

    private val map = mapOf(EAttr.name to name.toStr(), EAttr.type to type.code, EAttr.unique to unique, EAttr.list to list)

    @Suppress("UNCHECKED_CAST")
    override val keys: Set<ScalarAttr<out Any>> = map.keys

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getO(key: Attr<T>): T? = (map as Map<Attr<T>, T>)[key]

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
