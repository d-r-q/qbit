package qbit

data class Namespace(val name: String, val parent: Namespace? = null) {

    operator fun get(key: String) = Key(this, key)

    fun subNs(name: String) = Namespace(name, this)

}

data class Key(val ns: Namespace, val name: String)