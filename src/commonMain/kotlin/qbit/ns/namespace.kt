package qbit.ns

val root = Namespace(null, "")

fun ns(vararg parts: String) =
        Namespace.of(*parts)

data class Namespace(val parent: Namespace?, val name: String) {

    constructor(name: String) : this(root, name)

    companion object {
        fun of(vararg parts: String): Namespace {
            if (parts.isEmpty()) {
                throw IllegalArgumentException("parts is empty")
            }
            if (parts.size == 1 && parts[0] == "") {
                return root
            }

            val name = parts.last()
            val withoutRoot = parts.dropWhile { it == "" }
            val parent = withoutRoot.take(withoutRoot.size - 1)
                    .fold(root) { parent: Namespace?, n: String -> Namespace(parent, n) }
            return Namespace(parent, name)
        }
    }

    init {
        require(name != "" || parent == null)
    }

    val parts: List<String> =
            if (parent == null) listOf(name)
            else parent.parts + name

    operator fun get(key: String) = Key(this, key)

    operator fun invoke(subNs: String) = subNs(subNs)

    fun subNs(name: String) = Namespace(this, name)

    override fun toString(): String {
        return "${parent?.let { "$it." } ?: ""}$name"
    }

    fun isSubNs(parent: Namespace) =
            this.parent == parent

}

data class Key(val ns: Namespace, val name: String) {

    override fun toString(): String {
        return "$ns/$name"
    }
}