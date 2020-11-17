package qbit.storage

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import qbit.api.QBitException
import qbit.collections.PersistentMapStub
import qbit.ns.Key
import qbit.ns.Namespace
import qbit.spi.Storage

class MemStorage : Storage {

    private val data = atomic(PersistentMapStub<Namespace, PersistentMapStub<Key, ByteArray>>())

    override suspend fun add(key: Key, value: ByteArray) {
        if (key in nsMap(key)) {
            throw QBitException("Value with key $key already exists")
        }
        val newNs = nsMap(key).put(key, value)
        data.update { it.put(key.ns, newNs) }
    }

    override suspend fun overwrite(key: Key, value: ByteArray) {
        data.update { it.put(key.ns, nsMap(key).put(key, value)) }
    }

    private fun nsMap(key: Key): PersistentMapStub<Key, ByteArray> {
        val map: PersistentMapStub<Namespace, PersistentMapStub<Key, ByteArray>> = data.value
        return map[key.ns] ?: PersistentMapStub()
    }

    override fun load(key: Key): ByteArray? = data.value[key.ns]?.get(key)

    @Suppress("UNCHECKED_CAST")
    override fun keys(namespace: Namespace): Collection<Key> =
        (data.value[namespace]?.keys as? Collection<Key>) ?: setOf()

    override fun subNamespaces(namespace: Namespace): Collection<Namespace> =
        data.value.keys.asSequence()
            .filter { it.isSubNs(namespace) }
            .toList()

    override fun hasKey(key: Key): Boolean = nsMap(key).containsKey(key)

}