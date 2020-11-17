package qbit.trx

import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.serialization.NodeVal


interface TrxLog {

    val hash: Hash

    suspend fun append(facts: Collection<Eav>): TrxLog

}

class QTrxLog(private val head: NodeVal<Hash>, private val writer: Writer) : TrxLog {

    override val hash = head.hash

    override suspend fun append(facts: Collection<Eav>): TrxLog {
        val newHead = writer.store(head, facts)
        return QTrxLog(newHead, writer)
    }

}

