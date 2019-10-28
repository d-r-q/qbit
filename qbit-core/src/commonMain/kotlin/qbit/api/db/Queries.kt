package qbit.api.db

import qbit.api.model.Attr
import kotlin.reflect.KClass

sealed class Fetch

object Eager : Fetch()

object Lazy : Fetch()

abstract class QueryPred internal constructor() {

    abstract val attrName: String

    abstract fun compareTo(another: Any): Int

}

fun hasAttr(attr: Attr<*>): QueryPred =
        AttrPred(attr.name)

fun <T : Any> attrIs(attr: Attr<T>, value: T): QueryPred =
        AttrValuePred(attr.name, value)

fun <T : Any> attrIn(attr: Attr<T>, from: T, to: T): QueryPred =
        AttrRangePred(attr.name, from, to)

internal data class AttrPred(override val attrName: String) : QueryPred() {

    override fun compareTo(another: Any): Int = 0

}

internal data class AttrValuePred(override val attrName: String, val value: Any) : QueryPred() {

    override fun compareTo(another: Any): Int =
            qbit.index.compareValues(another, value)

}

internal data class AttrRangePred(override val attrName: String, val from: Any, val to: Any) : QueryPred() {

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

abstract class Query internal constructor() {

    abstract fun shouldFetch(attr: Attr<*>): Boolean

    abstract fun <ST : Any> subquery(subType: KClass<ST>): Query

}