package qbit.storage

class MemStorage : Storage {

    private val data = HashMap<Namespace, HashMap<Key, ByteArray>>()

    override fun store(key: Key, value: ByteArray) {
        data.getOrPut(key.ns, { HashMap() })[key] = value
    }

    override fun load(key: Key): ByteArray? = data[key.ns]?.get(key)

    override fun keys(namespace: Namespace): Collection<Key> = data[namespace]?.keys ?: setOf()

}