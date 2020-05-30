package qbit.collections


/**
 * Dump persistent map as stub for real persistent map from kotlinx-immutable while Kotlin 1.4 stabiliaztion
 */
class PersistentMapStub<K : Any, V : Any>(private val map: Map<K, V> = HashMap()) : PersistentMap<K, V>, Map<K, V> by map {

    override fun put(key: K, value: V): PersistentMapStub<K, V> {
        return PersistentMapStub(map.plus(key to value))
    }

    override fun remove(key: K): PersistentMap<K, V> {
        return PersistentMapStub(map.minus(key))
    }

    operator fun contains(key: K): Boolean {
        return containsKey(key)
    }

}