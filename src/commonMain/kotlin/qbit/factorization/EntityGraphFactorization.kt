package qbit.factorization

import qbit.model.Eav
import qbit.platform.IdentityHashMap


class EntityGraphFactorization(val entityFacts: IdentityHashMap<Any, List<Eav>>) : Iterable<Eav> {

    val size: Int =
            entityFacts.values.sumBy { it.size }

    override fun iterator(): Iterator<Eav> {
        return entityFacts.values.flatten().iterator()
    }

}