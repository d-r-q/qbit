package qbit

import qbit.mapping.destruct
import qbit.model.Attr2
import qbit.model.EID
import qbit.model.RoEntity
import qbit.model.toFacts
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.fail

fun dbOf(eids: Iterator<EID> = EID(0, 0).nextEids(), vararg entities: Any): Db {
    val facts = entities.flatMap { destruct(it, bootstrapSchema::get, eids) }
    return IndexDb(Index(facts.groupBy { it.eid }.map { it.key to it.value }))
}

fun dbOf(eids: Iterator<EID> = EID(0, 0).nextEids(), vararg entities: RoEntity<*>): Db {
    val facts = entities.flatMap { it.toFacts(eids.next()) }
    return IndexDb(Index(facts.groupBy { it.eid }.map { it.key to it.value }))
}

object emptyDb : Db {

    override fun pull(eid: EID): Map<String, List<Any>>? = null

    override fun <R : Any> pullT(eid: EID, type: KClass<R>): R? = null


    override fun query(vararg preds: QueryPred): Sequence<Map<String, List<Any>>> = emptySequence()

    override fun attr(attr: String): Attr2<*>? = bootstrapSchema[attr]

    override fun with(facts: List<Fact>): Db? {
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