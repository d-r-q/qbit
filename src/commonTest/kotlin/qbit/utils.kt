package qbit

import qbit.mapping.destruct
import qbit.model.*
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.fail

fun dbOf(eids: Iterator<Gid> = Gid(0, 0).nextGids(), vararg entities: Any): Db {
    val facts = entities.flatMap { destruct(it, bootstrapSchema::get, eids) }
    return IndexDb(Index(facts.groupBy { it.eid }.map { it.key to it.value }))
}

object emptyDb : Db {

    override fun pull(eid: Gid): Entity? = null

    override fun <R : Any> pullT(eid: Gid, type: KClass<R>): R? = null

    override fun queryGids(vararg preds: QueryPred): Sequence<Gid> = emptySequence()

    override fun query(vararg preds: QueryPred): Sequence<Entity> = emptySequence()

    override fun attr(attr: String): Attr<Any>? = bootstrapSchema[attr]

    override fun with(facts: List<Fact>): Db {
        return IndexDb(Index().addFacts(facts))
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