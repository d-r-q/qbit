package qbit.model

import qbit.Attrs
import qbit.ns.Key
import qbit.ns.Namespace

val qbitNs = Namespace.of("qbit")

// Interface

data class Attr<out T : Any>(val id: Gid?, val name: String, val type: Byte, val unique: Boolean, val list: Boolean) {

    init {
        check(!(unique and list))
    }

    fun id(id: Gid) = copy(id = id)

}

fun Attr<*>.toFacts(): List<Eav> = listOf(Eav(this.id!!, Attrs.name.name, this.name),
        Eav(this.id, Attrs.type.name, this.type),
        Eav(this.id, Attrs.unique.name, this.unique),
        Eav(this.id, Attrs.list.name, this.list))

// Utilities


infix fun <T : Any> Attr<T>.eq(v: T): AttrValue<Attr<T>, T> = QbitAttrValue(this, v)

private const val nsSep = "."
private const val keySep = "/"

internal fun Key.toStr() = this.ns.parts.joinToString(nsSep) + keySep + this.name

val tombstone = Attr<Boolean>(Gid(Iid(1, 4), 7), qbitNs["tombstone"].toStr(), QBoolean.code, unique = false, list = false)