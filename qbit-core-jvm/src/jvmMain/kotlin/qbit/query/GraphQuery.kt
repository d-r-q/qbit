package qbit.query

import qbit.api.db.Query
import qbit.api.model.Attr
import qbit.reflection.propertyFor
import kotlin.reflect.KClass

actual data class GraphQuery actual constructor(actual val type: KClass<*>, actual val links: Map<String, GraphQuery?>) : Query() {

    override fun shouldFetch(attr: Attr<*>): Boolean {
        return attr.name in links || type.propertyFor(attr)?.returnType?.isMarkedNullable == false
    }

    override fun <ST : Any> subquery(subType: KClass<ST>): Query = GraphQuery(subType, links)

}
