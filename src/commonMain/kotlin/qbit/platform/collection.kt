package qbit.platform

expect class IdentityHashMap<K, V>() {
    val values: MutableCollection<V>
    operator fun get(key: K): V?
    fun containsKey(key: K): Boolean
}

expect operator fun <K, V> IdentityHashMap<K, V>.set(key: K, value: V)

expect inline fun <K, V> IdentityHashMap<out K, V>.filterKeys(predicate: (K) -> Boolean): Map<K, V>
