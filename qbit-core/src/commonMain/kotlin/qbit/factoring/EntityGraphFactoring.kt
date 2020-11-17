package qbit.factoring

import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.Eav
import qbit.collections.IdentityMap

typealias Factor = (Any, (String) -> Attr<*>?, Iterator<Gid>) -> EntityGraphFactoring

class EntityGraphFactoring(val entityFacts: IdentityMap<Any, List<Eav>>) : Iterable<Eav> {

    val size: Int =
        entityFacts.values.sumBy { it.size }

    override fun iterator(): Iterator<Eav> {
        return entityFacts.values.flatten().iterator()
    }

}