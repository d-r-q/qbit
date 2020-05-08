package qbit.query

import qbit.api.db.Query
import qbit.api.model.Attr
import qbit.reflection.propertyFor
import kotlin.reflect.KClass

class EagerQuery : Query() {

    override fun shouldFetch(attr: Attr<*>): Boolean = true

    override fun <ST : Any> subquery(subType: KClass<ST>): Query = this

}

expect class GraphQuery(type: KClass<*>, links: Map<String, GraphQuery?>) : Query {
    val type: KClass<*>
    val links: Map<String, GraphQuery?>
}
