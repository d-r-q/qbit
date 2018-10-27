package qbit.schema

import qbit.*
import qbit.ns.Key
import qbit.ns.Namespace

val qbitAttrs = Namespace.of("qbit", "attrs")
val qbitInstance = Namespace.of("qbit", "instance")

val _name = Attr(qbitAttrs["name"], QString, unique = true)
val _type = Attr(qbitAttrs["type"], QByte)
val _unique = Attr(qbitAttrs["unique"], QBoolean)

val _forks = Attr(qbitInstance["forks"], QLong, false)
val _entities = Attr(qbitInstance["entities"], QLong, false)
val _iid = Attr(qbitInstance["iid"], QLong, true)

fun <T : Any> Attr(name: String, type: DataType<T>, unique: Boolean = false): Attr<T> = Attr(Key(name), type, unique)

data class Attr<T : Any>(val name: Key, val type: DataType<T>,
                         val unique: Boolean = false) : Entity {

    override fun <V : Any> set(key: Attr<V>, value: V): Attr<T> {
        var newName = name
        var newType = type
        var newUnique = unique
        when (key) {
            _name -> newName = Key(value as String)
            _type -> newType = DataType.ofCode(value as Byte) as DataType<T>
            _unique -> newUnique = value as Boolean
        }
        return Attr(newName, newType, newUnique)
    }

    override val keys: Set<Attr<*>>
        get() = setOf(_name, _type, _unique)

    override fun <T : Any> get(key: Attr<T>): T? {
        return when (key) {
            _name -> name.toStr() as T?
            _type -> type.code as T?
            _unique -> unique as T?
            else -> null
        }
    }

    val str = name.toStr()

    override fun toStored(eid: EID): StoredEntity =
            Entity(eid, listOf(_name to name.toStr(), _type to type.code, _unique to unique))

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