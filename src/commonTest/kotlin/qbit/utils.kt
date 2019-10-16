package qbit

import qbit.mapping.destruct
import qbit.model.*
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.fail

fun dbOf(eids: Iterator<Gid> = Gid(0, 0).nextGids(), vararg entities: Any): Db {
    val addedAttrs = entities
            .filterIsInstance<Attr<*>>()
            .map { it.name to it }
            .toMap()
    val facts = entities.flatMap { destruct(it, (bootstrapSchema + addedAttrs)::get, eids) }
    return IndexDb(Index(facts.groupBy { it.eid }.map { it.key to it.value }))
}

object emptyDb : Db {

    override fun pull(gid: Gid): StoredEntity? = null

    override fun <R : Any> pull(gid: Gid, type: KClass<R>, fetch: Fetch): R? = null

    override fun queryGids(vararg preds: QueryPred): Sequence<Gid> = emptySequence()

    override fun query(vararg preds: QueryPred): Sequence<Entity> = emptySequence()

    override fun attr(attr: String): Attr<Any>? = bootstrapSchema[attr]

    override fun with(facts: List<Fact>): Db {
        return IndexDb(Index().addFacts(facts))
    }

}

class EntityMapDb(private val map: Map<Gid, StoredEntity>) : Db {

    override fun pull(gid: Gid): StoredEntity? {
        return map[gid]
    }

    override fun <R : Any> pull(gid: Gid, type: KClass<R>, fetch: Fetch): R? {
        TODO("not implemented")
    }

    override fun query(vararg preds: QueryPred): Sequence<Entity> {
        TODO("not implemented")
    }

    override fun attr(attr: String): Attr<Any>? {
        TODO("not implemented")
    }

    override fun with(facts: List<Fact>): Db {
        TODO("not implemented")
    }

    override fun queryGids(vararg preds: QueryPred): Sequence<Gid> {
        TODO("not implemented")
    }

}

fun assertArrayEquals(arr1: Array<*>?, arr2: Array<*>?) {
    arr1!!; arr2!!
    assertEquals(arr1.size, arr2.size)
    (arr1 zip arr2).forEach { assertEquals(it.first, it.second) }
}

fun assertArrayEquals(arr1: ByteArray?, arr2: ByteArray?) {
    arr1!!; arr2!!
    assertEquals(arr1.size, arr2.size)
    (arr1 zip arr2).forEach { assertEquals(it.first, it.second) }
}

inline fun <reified E : Throwable> assertThrows(body: () -> Unit) {
    try {
        body()
        fail("${E::class} exception expected")
    } catch (e : Throwable) {
        if (e !is E) {
            throw e
        }
    }
}