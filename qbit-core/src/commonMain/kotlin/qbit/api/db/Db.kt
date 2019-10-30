package qbit.api.db

import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.Eav
import qbit.api.model.Entity
import qbit.api.model.StoredEntity
import kotlin.reflect.KClass

abstract class Db internal constructor() {

    abstract fun pull(gid: Gid): StoredEntity?

    abstract fun <R : Any> pull(gid: Gid, type: KClass<R>, fetch: Fetch = Lazy): R?

    // Todo: add check that attrs are presented in schema
    abstract fun query(vararg preds: QueryPred): Sequence<Entity>

    abstract fun attr(attr: String): Attr<Any>?

    abstract fun with(facts: Iterable<Eav>): Db

    abstract fun queryGids(vararg preds: QueryPred): Sequence<Gid>

}