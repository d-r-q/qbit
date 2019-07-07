package qbit.storage

import qbit.ns.Key
import qbit.ns.Namespace
import qbit.QBitException
import qbit.platform.ConcurrentHashMap
import qbit.platform.asSequence
import qbit.platform.getOrPut

class MemStorage : Storage {

    private val data = ConcurrentHashMap<Namespace, ConcurrentHashMap<Key, ByteArray>>()

    override fun add(key: Key, value: ByteArray) {
        val prev = nsMap(key).putIfAbsent(key, value)
        if (prev != null) {
            throw QBitException("Value with key $key already exists")
        }
    }

    override fun overwrite(key: Key, value: ByteArray) {
        nsMap(key).replace(key, value)
                ?: throw QBitException("Value with key $key does not exists")
    }

    private fun nsMap(key: Key) = data.getOrPut(key.ns) { ConcurrentHashMap() }

    override fun load(key: Key): ByteArray? = data[key.ns]?.get(key)

    override fun keys(namespace: Namespace): Collection<Key> = (data[namespace]?.keys as? Collection<Key>) ?: setOf()

    override fun subNamespaces(namespace: Namespace): Collection<Namespace> =
            data.keys.asSequence()
                    .filter { it.isSubNs(namespace) }
                    .toList()

    override fun hasKey(key: Key): Boolean = nsMap(key).containsKey(key)

}