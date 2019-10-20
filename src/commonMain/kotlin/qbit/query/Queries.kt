package qbit.query

import qbit.api.db.Query
import qbit.api.model.Attr
import qbit.reflection.propertyFor
import kotlin.reflect.KClass

class EagerQuery : Query() {

    override fun shouldFetch(attr: Attr<*>): Boolean = true

    override fun <ST : Any> subquery(subType: KClass<ST>): Query = this

}

data class GraphQuery(val type: KClass<*>, val links: Map<String, GraphQuery?>) : Query() {

    override fun shouldFetch(attr: Attr<*>): Boolean {
        return attr.name in links || type.propertyFor(attr)?.returnType?.isMarkedNullable == false
    }

    override fun <ST : Any> subquery(subType: KClass<ST>): Query = GraphQuery(subType, links)

}
