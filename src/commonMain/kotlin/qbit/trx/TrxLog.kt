package qbit.trx

import qbit.Hash
import qbit.serialization.NodeVal
import qbit.model.Fact


internal interface TrxLog {

    val hash: Hash

    fun append(facts: Collection<Fact>): TrxLog

}

internal class QTrxLog(private val head: NodeVal<Hash>, private val writer: Writer) : TrxLog {

    override val hash = head.hash

    override fun append(facts: Collection<Fact>): TrxLog {
        val newHead = writer.store(head, facts)
        return QTrxLog(newHead, writer)
    }

}

