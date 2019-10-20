package qbit.trx

import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.serialization.NodeVal


internal interface TrxLog {

    val hash: Hash

    fun append(facts: Collection<Eav>): TrxLog

}

internal class QTrxLog(private val head: NodeVal<Hash>, private val writer: Writer) : TrxLog {

    override val hash = head.hash

    override fun append(facts: Collection<Eav>): TrxLog {
        val newHead = writer.store(head, facts)
        return QTrxLog(newHead, writer)
    }

}

