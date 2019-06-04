package qbit.ns

import java.util.Arrays.asList

val root = Namespace(null, "")

fun ns(vararg parts: String) =
        Namespace.of(*parts)

data class Namespace(val parent: Namespace?, val name: String) {

    constructor(name: String) : this(null, name)

    companion object {
        fun of(vararg parts: String): Namespace {
            if (parts.isEmpty()) {
                throw IllegalArgumentException("parts is empty")
            }

            val name = parts.last()
            val parent = parts.take(parts.size - 1)
                    .fold(null) { parent: Namespace?, n: String -> Namespace(parent, n) }
            return Namespace(parent, name)
        }
    }

    val parts: List<String> =
            if (parent == null) asList(name)
            else parent.parts + name

    operator fun get(key: String) = Key(this, key)

    fun subNs(name: String) = Namespace(this, name)

    override fun toString(): String {
        return "${parent ?: ":"}.$name"
    }


}

data class Key(val ns: Namespace, val name: String) {

    override fun toString(): String {
        return "$ns/$name"
    }
}