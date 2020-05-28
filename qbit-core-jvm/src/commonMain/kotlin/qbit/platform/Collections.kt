package qbit.platform

expect class WeakHashMap<K, V>() {
    operator fun get(key: K): V?
}

expect operator fun <K, V> WeakHashMap<K, V>.set(key: K, value: V)