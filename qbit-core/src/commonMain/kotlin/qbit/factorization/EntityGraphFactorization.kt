package qbit.factorization

import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.Eav
import qbit.collections.IdentityMap

typealias Destruct = (Any, (String) -> Attr<*>?, Iterator<Gid>) -> EntityGraphFactorization

class EntityGraphFactorization(val entityFacts: IdentityMap<Any, List<Eav>>) : Iterable<Eav> {

    val size: Int =
            entityFacts.values.sumBy { it.size }

    override fun iterator(): Iterator<Eav> {
        return entityFacts.values.flatten().iterator()
    }

}