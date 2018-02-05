package qbit

import qbit.storage.NodesStorage

class Writer(private val db: Db, private val storage: NodesStorage, private val dbUuid: DbUuid) {

    fun store(e: List<Fact>): NodeVal<Hash> {
        try {
            return store(*e.toTypedArray())
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun store(vararg e: Fact): NodeVal<Hash> {
        try {
            return storage.store(Leaf(null, db.head, dbUuid, System.currentTimeMillis(), NodeData(e)))
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