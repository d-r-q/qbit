package qbit.trx

import qbit.model.Eav
import qbit.serialization.NodeVal
import qbit.model.Hash


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

