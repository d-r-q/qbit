package qbit.schema

import qbit.*
import qbit.ns.Key
import qbit.ns.Namespace
import java.lang.AssertionError

val qbitAttrs = Namespace.of("qbit", "attrs")
val qbitInstance = Namespace.of("qbit", "instance")

val _name: Attr<String> = ScalarAttr(qbitAttrs["name"], QString, unique = true)
val _type: Attr<Byte> = ScalarAttr(qbitAttrs["type"], QByte)
val _unique: Attr<Boolean> = ScalarAttr(qbitAttrs["unique"], QBoolean)

val _forks: Attr<Int> = ScalarAttr(qbitInstance["forks"], QInt, false)
val _entitiesCount: Attr<Int> = ScalarAttr(qbitInstance["entities"], QInt, false)
val _iid: Attr<Int> = ScalarAttr(qbitInstance["iid"], QInt, true)

internal fun Attr(name: String, type: DataType<*>, unique: Boolean = false): Attr<*> = ScalarAttr(Key(name), type, unique)

internal fun RefAttr(name: String, unique: Boolean = false): Attr<*> = RefAttr(Key(name), unique)

fun <T : Any> ScalarAttr(name: Key, type: DataType<T>, unique: Boolean = false): Attr<T> = ScalarAttrImpl(name, type, unique)

fun RefAttr(name: Key, unique: Boolean = false): RefAttr = RefAttrImpl(name, QEntity, unique)

interface Attr<T : Any> : Entitiable {

    val name: Key

    val type: DataType<T>

    val unique: Boolean

    fun str() = name.toStr()

    infix fun eq(v: T): AttrValue<Attr<T>, T>

}

private data class ScalarAttrImpl<T : Any>(override val name: Key, override val type: DataType<T>, override val unique: Boolean = false) : ScalarAttr<T>, Entitiable by AttrEntityImpl(name, type, unique)

private data class RefAttrImpl(override val name: Key, override val type: DataType<Entity>, override val unique: Boolean = false) : RefAttr, Entitiable by AttrEntityImpl(name, type, unique)

private data class AttrEntityImpl(val name: Key, val type: DataType<*>,
                                  val unique: Boolean = false) : Entitiable {

    @Suppress("UNCHECKED_CAST")
    override val keys: Set<Attr<Any>>
        get() = setOf(_name, _type, _unique) as Set<Attr<Any>>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: Attr<T>): T? {
        return when (key) {
            _name -> name.toStr() as T?
            _type -> type.code as T?
            _unique -> unique as T?
            else -> null
        }
    }

    // Attr object by definition cannot have refs
    override fun get(key: RefAttr): Entity? =
            null

}

interface RefAttr : Attr<Entity> {

    override infix fun eq(v: Entity): RefAttrValue = when (this) {
        is RefAttrImpl -> RefAttrValue(this, v)
        else -> throw AssertionError("Should never happen")
    }

}

interface ScalarAttr<T : Any> : Attr<T> {

    override infix fun eq(v: T): ScalarAttrValue<T> = when (this) {
        is ScalarAttrImpl -> ScalarAttrValue(this, v)
        else -> throw AssertionError("Should never happen")
    }

}

class Schema(private val attrs: Map<String, Attr<Any>>) {

    fun find(attrName: String): Attr<Any>? = attrs[attrName]

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