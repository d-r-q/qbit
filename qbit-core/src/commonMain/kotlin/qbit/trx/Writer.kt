package qbit.trx

import qbit.api.QBitException
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.platform.currentTimeMillis
import qbit.serialization.*

class Writer(private val storage: NodesStorage, private val dbUuid: DbUuid) {

    suspend fun store(head: Node<Hash>, e: Collection<Eav>): NodeVal<Hash> {
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