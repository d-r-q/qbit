package qbit.spi

import qbit.ns.Key
import qbit.ns.Namespace

interface Storage {

    suspend fun add(key: Key, value: ByteArray)

    suspend fun overwrite(key: Key, value: ByteArray)

    fun load(key: Key): ByteArray?

    fun keys(namespace: Namespace): Collection<Key>

    fun subNamespaces(namespace: Namespace): Collection<Namespace>

    fun hasKey(key: Key): Boolean

}
