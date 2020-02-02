package qbit.collections

class IdentityMap<K : Any, V> : Map<K, V> {

    private val impl = HashMap<Key<K>, V>()

    override val entries: Set<Map.Entry<K, V>>
        get() = impl.entries
            .map<Map.Entry<Key<K>, V>, Entry<K, V>>(
                ::Entry
            )
            .toSet()

    override val keys: Set<K>
        get() = impl.keys
            .map { it.key }
            .toSet()

    override val size: Int
        get() = impl.size

    override val values: Collection<V>
        get() = impl.values

    override fun containsKey(key: K): Boolean {
        return impl.containsKey(Key(key))
    }

    override fun containsValue(value: V): Boolean {
        return impl.containsValue(value)
    }

    override fun get(key: K): V? {
        return impl[Key(key)]
    }

    override fun isEmpty(): Boolean {
        return impl.isEmpty()
    }
    
    operator fun set(key: K, value: V) {
        impl.set(Key(key), value)
    }

    private class Key<K>(val key: K) {

        override fun equals(other: Any?): Boolean {
            return key === (other as? Key<*>)?.key
        }

        override fun hashCode(): Int {
            // put all keys in the same bucket as a common code way to shield from potential changes of hashCode of object
            return 0
        }

    }

    private class Entry<K, V>(private val e: Map.Entry<Key<K>, V>) : Map.Entry<K, V> {

        override val key: K
            get() = e.key.key

        override val value: V
            get() = e.value

    }

}

