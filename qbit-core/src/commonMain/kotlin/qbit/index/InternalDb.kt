package qbit.index

import qbit.api.db.Db
import qbit.api.db.QueryPred
import qbit.api.gid.Gid
import qbit.api.model.*


abstract class InternalDb : Db() {

    abstract fun pullEntity(gid: Gid): StoredEntity?

    // Todo: add check that attrs are presented in schema
    abstract fun query(vararg preds: QueryPred): Sequence<Entity>

    abstract fun with(facts: Iterable<Eav>, commitHash: Hash? = null, causalHashes: List<Hash> = emptyList()): InternalDb

    abstract fun attr(attr: String): Attr<Any>?

}