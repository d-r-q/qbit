package qbit.trx

import qbit.*
import qbit.db.DbUuid
import qbit.model.Fact
import qbit.platform.currentTimeMillis
import qbit.serialization.Leaf
import qbit.serialization.Node
import qbit.serialization.NodeData
import qbit.serialization.NodeVal
import qbit.storage.NodesStorage

class Writer(private val storage: NodesStorage, private val dbUuid: DbUuid) {

    fun store(head: Node<Hash>, e: Collection<Fact>): NodeVal<Hash> {
        try {
            if (!storage.hasNode(head)) {
                throw QBitException("Could not store child for node with hash=${head.hash}, because it's not exists in the storage")
            }
            return storage.store(Leaf(null, head, dbUuid, currentTimeMillis(), NodeData(e.toTypedArray())))
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

}