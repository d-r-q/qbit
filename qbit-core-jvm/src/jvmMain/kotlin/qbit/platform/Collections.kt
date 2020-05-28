package qbit.platform


actual typealias WeakHashMap<K, V> = java.util.WeakHashMap<K, V>

actual operator fun <K, V> WeakHashMap<K, V>.set(key: K, value: V) {
    put(key, value)
}