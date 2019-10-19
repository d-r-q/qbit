package qbit.platform

expect class IdentityHashMap<K, V>() : MutableMap<K, V>

expect class ConcurrentHashMap<K, V>() {
    fun putIfAbsent(key: K, value: V): V?
    fun replace(key: K, value: V): V?
    operator fun get(key: K): V?
    val keys: KeySetView<K, V>
    fun containsKey(key: K): Boolean
}

expect class KeySetView<K, V>

expect fun <K, V> KeySetView<K, V>.asSequence(): Sequence<K>

expect inline fun <K, V> ConcurrentHashMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V

expect class WeakHashMap<K, V>() {
    operator fun get(key: K): V?
}

expect operator fun <K, V> WeakHashMap<K, V>.set(key: K, value: V)