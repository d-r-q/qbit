package qbit.storage

import qbit.model.Key
import qbit.model.Namespace
import qbit.model.root

interface Storage {

    fun add(key: Key, value: ByteArray)

    fun overwrite(key: Key, value: ByteArray)

    fun load(key: Key): ByteArray?

    fun keys(namespace: Namespace): Collection<Key>

    fun subNamespaces(namespace: Namespace): Collection<Namespace>

    fun hasKey(key: Key): Boolean

}

internal fun copyStorage(from: Storage, to: Storage) {
    fun copyNs(ns: Namespace) {
        from.keys(ns).forEach {
            to.add(it, from.load(it)!!)
        }
        from.subNamespaces(ns).forEach {
            copyNs(it)
        }
    }
    copyNs(root)
}
