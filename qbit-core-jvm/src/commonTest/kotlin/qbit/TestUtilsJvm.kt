package qbit

import qbit.api.db.Fetch
import qbit.api.db.QueryPred
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.api.model.Eav
import qbit.api.model.Entity
import qbit.api.model.StoredEntity
import qbit.index.Index
import qbit.index.IndexDb
import qbit.index.InternalDb
import qbit.test.model.testsSerialModule
import kotlin.reflect.KClass
import kotlin.test.assertEquals

internal fun dbOf(eids: Iterator<Gid> = Gid(0, 0).nextGids(), vararg entities: Any): InternalDb {
    val addedAttrs = entities
            .filterIsInstance<Attr<*>>()
            .map { it.name to it }
            .toMap()
    val facts = entities.flatMap { testSchemaFactorizer.factor(it, (bootstrapSchema + addedAttrs)::get, eids) }
    return IndexDb(Index(facts.groupBy { it.gid }.map { it.key to it.value }), testsSerialModule)
}

internal object EmptyDb : InternalDb() {

    override fun pullEntity(gid: Gid): StoredEntity? = null

    override fun <R : Any> pull(gid: Gid, type: KClass<R>, fetch: Fetch): R? = null

    override fun queryGids(vararg preds: QueryPred): Sequence<Gid> = emptySequence()

    override fun query(vararg preds: QueryPred): Sequence<Entity> = emptySequence()

    override fun attr(attr: String): Attr<Any>? = bootstrapSchema[attr]

    override fun with(facts: Iterable<Eav>): InternalDb {
        return IndexDb(Index().addFacts(facts), testsSerialModule)
    }

}

fun assertArrayEquals(arr1: ByteArray?, arr2: ByteArray?) {
    arr1!!; arr2!!
    assertEquals(arr1.size, arr2.size)
    (arr1 zip arr2).forEach { assertEquals(it.first, it.second) }
}

