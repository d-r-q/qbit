package qbit

import qbit.storage.NodesStorage

class Writer(private val db: Db, private val storage: NodesStorage, private val dbUuid: DbUuid) {

    fun store(e: List<Fact>): NodeVal {
        try {
            return store(*e.toTypedArray())
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun store(vararg e: Fact): NodeVal {
        try {
            val newHead = addNode(NodeData(e))
            storage.store(newHead)
            return newHead
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    private fun addNode(data: NodeData): Leaf = Leaf(db.head, dbUuid, System.currentTimeMillis(), data)

    fun append(n: Node): NodeVal {
        return when (n) {
            is NodeRef -> {
                return storage.load(n) ?: throw QBitException("Could not resolve node $n")
            }
            is Leaf -> {
                val parent = append(n.parent)
                storage.store(n)
                Leaf(parent, n.source, n.timestamp, n.data)
            }
            is Merge -> {
                val parent1 = append(n.parent1)
                val parent2 = append(n.parent2)
                storage.store(n)
                Merge(parent1, parent2, n.source, n.timestamp, n.data)
            }
            is Root -> {
                storage.store(n)
                n
            }
        }
    }
}