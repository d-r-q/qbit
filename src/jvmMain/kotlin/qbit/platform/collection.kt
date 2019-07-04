package qbit.platform


actual typealias IdentityHashMap<K, V> = java.util.IdentityHashMap<K, V>

actual inline fun <K, V> IdentityHashMap<out K, V>.filterKeysImpl(predicate: (K) -> Boolean): Map<K, V> {
    return this.filterKeys(predicate)
}

actual operator fun <K, V> IdentityHashMap<K, V>.set(key: K, value: V) {
    put(key, value)
}