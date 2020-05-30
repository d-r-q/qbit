package qbit.collections


class LimitedPersistentMap<K : Any, V : Any>(
    private val limit: Int,
    private val delegate: PersistentMap<K, V> = PersistentMapStub()
) : PersistentMap<K, V> by delegate {

    override fun put(key: K, value: V): LimitedPersistentMap<K, V> {
        val limited = if (delegate.size < limit) {
            this.delegate
        } else {
            this.delegate.remove(this.keys.first())
        }
        return LimitedPersistentMap(limit, limited.put(key, value))
    }

}