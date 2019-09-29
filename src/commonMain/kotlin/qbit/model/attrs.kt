package qbit.model

import qbit.Attrs
import qbit.Fact
import qbit.QBitException
import qbit.mapping.types
import qbit.ns.Key

// Interface

data class Attr2<out T : Any>(val id: EID?, val name: String, val type: Byte, val unique: Boolean, val list: Boolean)

inline fun <reified T : Any> Attr(name: String, unique: Boolean = true) = Attr2<T>(
        null,
        name,
        types[T::class]?.code ?: throw QBitException("Unsupported type: ${T::class}"),
        unique,
        false
)

fun Attr2<*>.toFacts(): List<Fact> = listOf(Fact(this.id!!, Attrs.name.name, this.name),
        Fact(this.id, Attrs.type.name, this.type),
        Fact(this.id, Attrs.unique.name, this.unique),
        Fact(this.id, Attrs.list.name, this.list))

// Utilities


infix fun <T : Any> Attr2<T>.eq(v: T): AttrValue<Attr2<T>, T> = QbitAttrValue(this, v)

private const val nsSep = "."
private const val keySep = "/"

internal fun Key.toStr() = this.ns.parts.joinToString(nsSep) + keySep + this.name

