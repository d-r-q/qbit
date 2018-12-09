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
val _entities: Attr<Int> = ScalarAttr(qbitInstance["entities"], QInt, false)
val _iid: Attr<Long> = ScalarAttr(qbitInstance["iid"], QLong, true)

internal fun Attr(name: String, type: DataType<*>, unique: Boolean = false): Attr<*> = ScalarAttr(Key(name), type, unique)

internal fun RefAttr(name: String, unique: Boolean = false): Attr<*> = RefAttr(Key(name), unique)

fun <T : Any> ScalarAttr(name: Key, type: DataType<T>, unique: Boolean = false): Attr<T> = ScalarAttrImpl(name, type, unique)

fun RefAttr(name: Key, unique: Boolean = false): RefAttr = RefAttrImpl(name, QEntity, unique)

interface Attr<T : Any> : Entity {

    val name: Key

    val type: DataType<T>

    val unique: Boolean

    fun str() = name.toStr()

    infix fun eq(v: T): AttrValue<Attr<T>, T> = when (this) {
        is ScalarAttrImpl -> ScalarAttrValue(this, v)
        is RefAttr -> RefAttrValue(this, v as Entity) as AttrValue<Attr<T>, T>
        else -> throw AssertionError("Should never happen")
    }

}

private data class ScalarAttrImpl<T : Any>(override val name: Key, override val type: DataType<T>, override val unique: Boolean = false) : Attr<T>, Entity by AttrEntityImpl(name, type, unique)

private data class RefAttrImpl(override val name: Key, override val type: DataType<Entity>, override val unique: Boolean = false) : RefAttr, Entity by AttrEntityImpl(name, type, unique)

private data class AttrEntityImpl(val name: Key, val type: DataType<*>,
                                  val unique: Boolean = false) : Entity {
    override fun <V : Any> set(key: Attr<V>, value: V): AttrEntityImpl {
        var newName = name
        var newType = type
        var newUnique = unique
        when (key) {
            _name -> newName = Key(value as String)
            _type -> newType = DataType.ofCode(value as Byte) as DataType<*>
            _unique -> newUnique = value as Boolean
        }
        return AttrEntityImpl(newName, newType, newUnique)
    }

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

    override fun get(key: RefAttr): Entity? =
            null

    override fun set(key: RefAttr, value: Entity): Entity {
        TODO("Attribute attrs couldn't refer other entites yet")
    }

    override fun toIdentified(eid: EID): IdentifiedEntity =
            Entity(_name eq name.toStr(), _type eq type.code, _unique eq unique).toIdentified(eid)

}

interface RefAttr : Attr<Entity>

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