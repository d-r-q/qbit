package qbit.collections

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet


/**
 * Dump persistent map as stub for real persistent map from kotlinx-immutable while Kotlin 1.4 stabiliaztion
 */
class PersistentMap<K : Any, V : Any> {

    private val mapRef: AtomicRef<Map<K, V>> = atomic(HashMap())

    val keys: Set<K>
        get() = mapRef.value.keys

    fun put(key: K, value: V): PersistentMap<K, V> {
        mapRef.updateAndGet {
            it.plus(key to value)
        }
        return this
    }

    operator fun get(key: K): V? {
        return mapRef.value[key]
    }

    fun containsKey(key: K): Boolean {
        return mapRef.value.containsKey(key)
    }

    operator fun contains(key: K): Boolean {
        return containsKey(key)
    }


}