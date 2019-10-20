package qbit.query

import qbit.model.Attr
import qbit.reflection.propertyFor
import kotlin.reflect.KClass

sealed class Fetch

object Eager : Fetch()

object Lazy : Fetch()

interface QueryPred {
    val attrName: String

    fun compareTo(another: Any): Int
}

fun hasAttr(attr: Attr<*>): QueryPred =
        AttrPred(attr.name)

fun <T : Any> attrIs(attr: Attr<T>, value: T): QueryPred =
        AttrValuePred(attr.name, value)

fun <T : Any> attrIn(attr: Attr<T>, from: T, to: T): QueryPred =
        AttrRangePred(attr.name, from, to)

internal data class AttrPred(override val attrName: String) : QueryPred {

    override fun compareTo(another: Any): Int = 0

}

internal data class AttrValuePred(override val attrName: String, val value: Any) : QueryPred {

    override fun compareTo(another: Any): Int =
            qbit.index.compareValues(another, value)

}

internal data class AttrRangePred(override val attrName: String, val from: Any, val to: Any) : QueryPred {

    override fun compareTo(another: Any): Int {
        val r1 = qbit.index.compareValues(another, from)
        if (r1 < 0) {
            return r1
        }
        val r2 = qbit.index.compareValues(another, to)
        if (r2 > 0) {
            return r2
        }
        return 0
    }

}

interface Query<T> {

    fun shouldFetch(attr: Attr<*>): Boolean

    fun <ST : Any> subquery(subType: KClass<ST>): Query<ST>

}

class EagerQuery<T> : Query<T> {

    override fun shouldFetch(attr: Attr<*>): Boolean = true

    override fun <ST : Any> subquery(subType: KClass<ST>): Query<ST> = this as Query<ST>

}

data class GraphQuery<R : Any>(val type: KClass<R>, val links: Map<String, GraphQuery<*>?>) : Query<R> {

    override fun shouldFetch(attr: Attr<*>): Boolean {
        return attr.name in links || type.propertyFor(attr)?.returnType?.isMarkedNullable == false
    }

    override fun <ST : Any> subquery(subType: KClass<ST>): Query<ST> = GraphQuery(subType, links)

}
