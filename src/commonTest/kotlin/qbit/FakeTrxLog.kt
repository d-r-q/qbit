package qbit

import qbit.trx.TrxLog

internal class FakeTrxLog() : TrxLog {

    var appendsCalls = 0

    val appendedFacts = ArrayList<Collection<Fact>>()
    override val hash: Hash
        get() = TODO("not implemented")

    override fun append(facts: Collection<Fact>): TrxLog {
        appendsCalls++
        appendedFacts.add(facts)
        return this
    }

}
