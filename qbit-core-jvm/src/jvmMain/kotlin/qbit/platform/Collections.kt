package qbit.platform

import kotlin.collections.asSequence as asSequenceImpl
import kotlin.collections.getOrPut as getOrPutImpl


actual typealias ConcurrentHashMap<K, V> = java.util.concurrent.ConcurrentHashMap<K, V>

actual typealias KeySetView<K, V> = java.util.concurrent.ConcurrentHashMap.KeySetView<K, V>

actual inline fun <K, V> ConcurrentHashMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    return getOrPutImpl(key, defaultValue)
}

actual fun <K, V> KeySetView<K, V>.asSequence(): Sequence<K> {
   return asSequenceImpl()
}

actual typealias WeakHashMap<K, V> = java.util.WeakHashMap<K, V>

actual operator fun <K, V> WeakHashMap<K, V>.set(key: K, value: V) {
    put(key, value)
}