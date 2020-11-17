package qbit.api.db

import qbit.api.gid.Gid
import kotlin.reflect.KClass

abstract class Db internal constructor() {

    abstract fun <R : Any> pull(gid: Gid, type: KClass<R>, fetch: Fetch = Lazy): R?

    abstract fun queryGids(vararg preds: QueryPred): Sequence<Gid>

}

inline fun <reified R : Any> Db.pull(eid: Gid): R? {
    return this.pull(eid, R::class)
}

inline fun <reified R : Any> Db.pull(eid: Long): R? {
    return this.pull(Gid(eid), R::class)
}

inline fun <reified R : Any> Db.query(vararg preds: QueryPred, fetch: Fetch = Lazy): Sequence<R> =
    this.queryGids(*preds).map { this.pull(it, R::class, fetch)!! }
