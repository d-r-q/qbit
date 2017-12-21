package qbit.storage

import java.util.function.BiFunction

interface Storage {

    fun store(key: Key, value: ByteArray)

    fun load(key: Key): ByteArray?

    fun keys(namespace: Namespace): Collection<Key>

}

data class Namespace(val name: String) {

    operator fun get(key: String) = Key(this, key)

}

data class Key(val ns: Namespace, val key: String)