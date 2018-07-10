package qbit.schema

import qbit.*

val qbitAttrs = Namespace("qbit").subNs("attrs")
val qbitInstance = Namespace.of("qbit", "instance")

val _name = Attr(qbitAttrs["name"], QString, unique = true)
val _type = Attr(qbitAttrs["type"], QByte)
val _unique = Attr(qbitAttrs["unique"], QByte)

val _forks = Attr(qbitInstance["forks"], QLong, false)
val _entities = Attr(qbitInstance["entities"], QLong, false)
val _iid = Attr(qbitInstance["iid"], QLong, true)

fun <T : Any> Attr(name: String, type: DataType<T>, unique: Boolean = false): Attr<T> = Attr(Key(name), type, unique)

data class Attr<T : Any>(val name: Key, val type: DataType<T>,
                         val unique: Boolean = false) : Entity {

    override val keys: Set<Attr<*>>
        get() = setOf(_name, _type, _unique)

    override fun get(key: Attr<*>): Any? {
        return when (key) {
            _name -> name.toStr()
            _type -> type.code
            _unique -> unique
            else -> null
        }
    }

    val str = name.toStr()

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