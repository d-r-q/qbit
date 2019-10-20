package qbit.trx

import qbit.model.DbUuid
import qbit.model.Eav
import qbit.model.QBitException
import qbit.platform.currentTimeMillis
import qbit.serialization.*
import qbit.model.Hash

class Writer(private val storage: NodesStorage, private val dbUuid: DbUuid) {

    fun store(head: Node<Hash>, e: Collection<Eav>): NodeVal<Hash> {
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