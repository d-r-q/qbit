package qbit.storage

import qbit.api.QBitException
import qbit.collections.PersistentMap
import qbit.ns.Key
import qbit.ns.Namespace
import qbit.spi.Storage

class MemStorage : Storage {

    private val data = kotlinx.atomicfu.atomic(PersistentMap<Namespace, PersistentMap<Key, ByteArray>>())

    override suspend fun add(key: Key, value: ByteArray) {
        if (key in nsMap(key)) {
            throw QBitException("Value with key $key already exists")
        }
        val newNs = nsMap(key).put(key, value)
        data.compareAndSet(data.value, data.value.put(key.ns, newNs))
    }

    override suspend fun overwrite(key: Key, value: ByteArray) {
        nsMap(key).put(key, value)
    }

    private fun nsMap(key: Key): PersistentMap<Key, ByteArray> {
        val map: PersistentMap<Namespace, PersistentMap<Key, ByteArray>> = data.value
        return map[key.ns] ?: PersistentMap()
    }

    override fun load(key: Key): ByteArray? = data.value[key.ns]?.get(key)

    @Suppress("UNCHECKED_CAST")
    override fun keys(namespace: Namespace): Collection<Key> = (data.value[namespace]?.keys as? Collection<Key>) ?: setOf()

    override fun subNamespaces(namespace: Namespace): Collection<Namespace> =
            data.value.keys.asSequence()
                    .filter { it.isSubNs(namespace) }
                    .toList()

    override fun hasKey(key: Key): Boolean = nsMap(key).containsKey(key)

}