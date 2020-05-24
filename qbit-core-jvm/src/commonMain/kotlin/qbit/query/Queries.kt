package qbit.query

import qbit.api.db.Query
import qbit.api.model.Attr
import kotlin.reflect.KClass

data class GraphQuery constructor(val type: KClass<*>, val links: Map<String, GraphQuery?>) : Query() {

    override fun shouldFetch(attr: Attr<*>): Boolean {
        return true
    }

    override fun <ST : Any> subquery(subType: KClass<ST>): Query = GraphQuery(subType, links)

}
