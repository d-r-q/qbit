package qbit.model

internal val qbitNs = Namespace.of("qbit")

// Interface

data class Attr<out T : Any>(val id: Gid?, val name: String, val type: Byte, val unique: Boolean, val list: Boolean) {

    init {
        check(!(unique and list))
    }

    fun id(id: Gid) = copy(id = id)

}

// Utilities


infix fun <T : Any> Attr<T>.eq(v: T): AttrValue<Attr<T>, T> = QbitAttrValue(this, v)

private const val nsSep = "."
private const val keySep = "/"

internal fun Key.toStr() = this.ns.parts.joinToString(nsSep) + keySep + this.name

