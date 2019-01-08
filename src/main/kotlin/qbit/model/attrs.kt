package qbit.model

import qbit.ns.Key
import qbit.ns.Namespace

val qbitAttrs = Namespace.of("qbit", "attrs")
val qbitInstance = Namespace.of("qbit", "instance")

val _name: Attr<String> = ScalarAttr(qbitAttrs["name"], DataType.QString, unique = true)
val _type: Attr<Byte> = ScalarAttr(qbitAttrs["type"], DataType.QByte)
val _unique: Attr<Boolean> = ScalarAttr(qbitAttrs["unique"], DataType.QBoolean)

val _forks: Attr<Int> = ScalarAttr(qbitInstance["forks"], DataType.QInt, false)
val _entitiesCount: Attr<Int> = ScalarAttr(qbitInstance["entities"], DataType.QInt, false)
val _iid: Attr<Long> = ScalarAttr(qbitInstance["iid"], DataType.QLong, true)

internal fun Attr(name: String, type: DataType<*>, unique: Boolean = false): Attr<*> = ScalarAttr(AttrKey.Key(name), type, unique)

internal fun RefAttr(name: String, unique: Boolean = false): Attr<*> = RefAttr(AttrKey.Key(name), unique)

fun <T : Any> ScalarAttr(name: Key, type: DataType<T>, unique: Boolean = false): Attr<T> = ScalarAttrImpl(name, type, unique)

fun RefAttr(name: Key, unique: Boolean = false): RefAttr = RefAttrImpl(name, DataType.QEntity, unique)

interface Attr<T : Any> {

    val name: Key

    val type: DataType<T>

    val unique: Boolean

    fun str() = AttrKey.toStr(name)

}

interface RefAttr : Attr<Any>

interface ScalarAttr<T : Any> : Attr<T>


private data class ScalarAttrImpl<T : Any>(override val name: Key, override val type: DataType<T>, override val unique: Boolean = false) : ScalarAttr<T>

private data class RefAttrImpl(override val name: Key, override val type: DataType<Any>, override val unique: Boolean = false) : RefAttr

private data class AttrEntityImpl(val name: Key, val type: DataType<*>,
                                  val unique: Boolean = false) : Entitiable {

    @Suppress("UNCHECKED_CAST")
    override val keys: Set<Attr<Any>>
        get() = setOf(_name, _type, _unique) as Set<Attr<Any>>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: Attr<T>): T? {
        return when (key) {
            _name -> AttrKey.toStr(name) as T?
            _type -> type.code as T?
            _unique -> unique as T?
            else -> null
        }
    }

}


object AttrKey {
    private const val nsSep = "."
    private const val keySep = "/"

    fun toStr(key: Key) = key.ns.parts.joinToString(nsSep) + keySep + key.name

    fun Key(keyStr: String): Key {
        val parts = keyStr.split(keySep)
        if (parts.size != 2) {
            throw IllegalArgumentException("Malformed attribute name: $keyStr")
        }
        val (ns, name) = parts
        return Namespace.of(*ns.split(nsSep).toTypedArray())[name]
    }
}
