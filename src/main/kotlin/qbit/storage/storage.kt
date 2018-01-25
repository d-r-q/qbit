package qbit.storage

interface Storage {

    fun add(key: Key, value: ByteArray)

    fun overwrite(key: Key, value: ByteArray)

    fun load(key: Key): ByteArray?

    fun keys(namespace: Namespace): Collection<Key>

    fun hasKey(key: Key): Boolean

}

data class Namespace(val name: String, val parent: Namespace? = null) {

    operator fun get(key: String) = Key(this, key)

    fun subNs(name: String) = Namespace(name, this)

}

data class Key(val ns: Namespace, val name: String)