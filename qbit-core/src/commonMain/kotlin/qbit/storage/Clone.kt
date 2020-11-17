package qbit.storage

import qbit.ns.Namespace
import qbit.ns.root
import qbit.spi.Storage


// Workaround for: https://github.com/Kotlin/kotlinx.coroutines/issues/2067
class CloneStorage(private val from: Storage, private val to: Storage) {

    suspend operator fun invoke() {
        copyNs(root)
    }

    private suspend fun copyNs(ns: Namespace) {
        from.keys(ns).forEach {
            to.add(it, from.load(it)!!)
        }
        from.subNamespaces(ns).forEach {
            copyNs(it)
        }
    }

}