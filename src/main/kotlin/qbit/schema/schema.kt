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

private const val nsSep = "."
private const val keySep = "/"

data class Attr<T : Any>(val name: Key, val type: DataType<T>,
                         val unique: Boolean = false) : Entity,
        Map<Attr<*>, Any> by mapOf(_name to name.toStr(),
                _type to type.code,
                _unique to unique) {

    val str: String = this[_name] as String

}

class Schema(private val attrs: List<Attr<*>>) {

    fun find(attrName: Key): Attr<*>? = attrs
            .firstOrNull { it.name == attrName }

}

private fun Key.toStr() = this.ns.parts.joinToString(nsSep) + keySep + this.name

fun parseAttrName(keyStr: String): Key {
    val parts = keyStr.split(keySep)
    if (parts.size != 2) {
        throw IllegalArgumentException("Malformed attribute name: $keyStr")
    }
    val (ns, name) = parts
    return Namespace.of(*ns.split(nsSep).toTypedArray())[name]
}
