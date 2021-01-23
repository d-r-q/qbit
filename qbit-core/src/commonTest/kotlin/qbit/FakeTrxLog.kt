package qbit

import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.serialization.Node
import qbit.serialization.NodeVal
import qbit.trx.TrxLog

internal class FakeTrxLog(override val hash: Hash = Hash(byteArrayOf(1))) : TrxLog {

    var appendsCalls = 0

    val appendedFacts = ArrayList<Collection<Eav>>()

    override suspend fun append(facts: Collection<Eav>): TrxLog {
        appendsCalls++
        appendedFacts.add(facts)
        return this
    }

    override fun nodesSince(to: Hash, resolveNode: (Node<Hash>) -> NodeVal<Hash>?): List<NodeVal<Hash>> {
        TODO("Not yet implemented")
    }

}
