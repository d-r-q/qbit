package qbit.storage

import qbit.ns.Namespace
import qbit.ns.root
import qbit.spi.Storage


internal fun cloneStorage(from: Storage, to: Storage) {
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