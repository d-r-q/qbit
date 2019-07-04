package qbit

import qbit.platform.currentTimeMillis
import qbit.storage.NodesStorage

class Writer(private val storage: NodesStorage, private val dbUuid: DbUuid) {

    fun store(head: Node<Hash>, e: Collection<Fact>): NodeVal<Hash> {
        try {
            return store(head, *e.toTypedArray())
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun store(head: Node<Hash>, vararg e: Fact): NodeVal<Hash> {
        try {
            if (!storage.hasNode(head)) {
                throw QBitException("Could not store child for node with hash=${head.hash}, because it's not exists in the storage")
            }
            return storage.store(Leaf(null, head, dbUuid, currentTimeMillis(), NodeData(e)))
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun appendNode(n: NodeVal<Hash?>): NodeVal<Hash> {
        return storage.store(n)
    }

    fun appendGraph(n: Node<Hash>): NodeVal<Hash> {
        return when (n) {
            is NodeRef -> {
                return storage.load(n) ?: throw QBitException("Could not resolve node $n")
            }
            is Leaf -> {
                val parent = appendGraph(n.parent)
                val stored = storage.store(n)
                Leaf(stored.hash, parent, n.source, n.timestamp, n.data)
            }
            is Merge -> {
                val parent1 = appendGraph(n.parent1)
                val parent2 = appendGraph(n.parent2)
                val stored = storage.store(n)
                Merge(stored.hash, parent1, parent2, n.source, n.timestamp, n.data)
            }
            is Root -> {
                storage.store(n)
                n
            }
        }
    }
}