package qbit

import qbit.model.Fact
import qbit.trx.TrxLog

internal class FakeTrxLog(override val hash: Hash = Hash(byteArrayOf(1))) : TrxLog {

    var appendsCalls = 0

    val appendedFacts = ArrayList<Collection<Fact>>()

    override fun append(facts: Collection<Fact>): TrxLog {
        appendsCalls++
        appendedFacts.add(facts)
        return this
    }

}
