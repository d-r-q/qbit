package qbit.storage

import qbit.ns.Key
import qbit.ns.Namespace
import qbit.QBitException
import java.util.concurrent.ConcurrentHashMap

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

    private fun nsMap(key: Key) = data.getOrPut(key.ns, { ConcurrentHashMap() })

    override fun load(key: Key): ByteArray? = data[key.ns]?.get(key)

    override fun keys(namespace: Namespace): Collection<Key> = data[namespace]?.keys ?: setOf()

    override fun hasKey(key: Key): Boolean = nsMap(key).containsKey(key)

}