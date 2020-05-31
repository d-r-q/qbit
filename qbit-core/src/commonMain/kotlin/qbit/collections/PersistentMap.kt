package qbit.collections


interface PersistentMap<K : Any, V : Any> : Map<K, V> {

    fun put(key: K, value: V): PersistentMap<K, V>

    fun remove(key: K): PersistentMap<K, V>

}