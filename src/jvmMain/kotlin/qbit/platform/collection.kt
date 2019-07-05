package qbit.platform

import kotlin.collections.filterKeys as filterKeysImpl

actual typealias IdentityHashMap<K, V> = java.util.IdentityHashMap<K, V>

actual inline fun <K, V> IdentityHashMap<out K, V>.filterKeys(predicate: (K) -> Boolean): Map<K, V> {
    return this.filterKeysImpl(predicate)
}

actual operator fun <K, V> IdentityHashMap<K, V>.set(key: K, value: V) {
    put(key, value)
}