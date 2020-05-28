package qbit.storage

import qbit.api.QBitException
import qbit.ns.Key
import qbit.ns.Namespace
import qbit.spi.Storage

class MemStorage : Storage {

    private val data = HashMap<Namespace, HashMap<Key, ByteArray>>()

    override suspend fun add(key: Key, value: ByteArray) {
        if (key in nsMap(key)) {
            throw QBitException("Value with key $key already exists")
        }
        val prev = nsMap(key).put(key, value)
    }

    override suspend fun overwrite(key: Key, value: ByteArray) {
        nsMap(key).put(key, value)
    }

    private fun nsMap(key: Key) = data.getOrPut(key.ns) { HashMap() }

    override fun load(key: Key): ByteArray? = data[key.ns]?.get(key)

    @Suppress("UNCHECKED_CAST")
    override fun keys(namespace: Namespace): Collection<Key> = (data[namespace]?.keys as? Collection<Key>) ?: setOf()

    override fun subNamespaces(namespace: Namespace): Collection<Namespace> =
            data.keys.asSequence()
                    .filter { it.isSubNs(namespace) }
                    .toList()

    override fun hasKey(key: Key): Boolean = nsMap(key).containsKey(key)

}