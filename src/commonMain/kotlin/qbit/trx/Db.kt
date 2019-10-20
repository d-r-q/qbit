package qbit.trx

import qbit.query.Fetch
import qbit.query.Lazy
import qbit.query.QueryPred
import qbit.model.*
import kotlin.reflect.KClass

interface Db {

    fun pull(gid: Gid): StoredEntity?

    fun <R : Any> pull(gid: Gid, type: KClass<R>, fetch: Fetch = Lazy): R?

    // Todo: add check that attrs are presented in schema
    fun query(vararg preds: QueryPred): Sequence<Entity>

    fun attr(attr: String): Attr<Any>?

    fun with(facts: Iterable<Eav>): Db

    fun queryGids(vararg preds: QueryPred): Sequence<Gid>

}