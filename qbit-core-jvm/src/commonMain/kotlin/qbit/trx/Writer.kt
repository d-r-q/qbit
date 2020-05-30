package qbit.trx

import qbit.api.QBitException
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.api.model.nullHash
import qbit.api.system.DbUuid
import qbit.platform.currentTimeMillis
import qbit.serialization.Leaf
import qbit.serialization.Node
import qbit.serialization.NodeData
import qbit.serialization.NodeVal
import qbit.serialization.NodesStorage

class Writer(private val storage: NodesStorage, private val dbUuid: DbUuid) {

    suspend fun store(head: Node, e: Collection<Eav>): NodeVal {
        try {
            if (!storage.hasNode(head)) {
                throw QBitException("Could not store child for node with hash=${head.hash}, because it's not exists in the storage")
            }
            return storage.store(Leaf(nullHash, head, dbUuid, currentTimeMillis(), NodeData(e.toTypedArray())))
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

}