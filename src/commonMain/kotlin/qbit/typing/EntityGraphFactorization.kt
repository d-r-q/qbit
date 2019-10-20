package qbit.typing

import qbit.model.Fact
import qbit.platform.IdentityHashMap


class EntityGraphFactorization(val entityFacts: IdentityHashMap<Any, List<Fact>>) : Iterable<Fact> {

    val size: Int =
            entityFacts.values.sumBy { it.size }

    override fun iterator(): Iterator<Fact> {
        return entityFacts.values.flatten().iterator()
    }

}