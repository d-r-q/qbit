package qbit

import qbit.model.Eav
import qbit.trx.TrxLog
import qbit.util.Hash

internal class FakeTrxLog(override val hash: Hash = Hash(byteArrayOf(1))) : TrxLog {

    var appendsCalls = 0

    val appendedFacts = ArrayList<Collection<Eav>>()

    override fun append(facts: Collection<Eav>): TrxLog {
        appendsCalls++
        appendedFacts.add(facts)
        return this
    }

}
