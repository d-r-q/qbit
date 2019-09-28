package qbit

import qbit.model.*
import kotlin.reflect.KClass
import kotlin.test.assertEquals

fun dbOf(eids: Iterator<EID> = EID(0, 0).nextEids(), vararg entities: RoEntity<*>): Db {
    val facts = entities.flatMap { it.toFacts(eids.next()) }
    return IndexDb(Index(facts.groupBy { it.eid }.map { it.key to it.value }), Hash(byteArrayOf()))
}

object emptyDb : Db {
    override val hash: Hash = nullHash

    override fun pull(eid: EID): StoredEntity? = null

    override fun <R : Any> pullT(eid: EID, type: KClass<R>): R? = null


    override fun query(vararg preds: QueryPred): Sequence<StoredEntity> = emptySequence()

    override fun attr(attr: String): Attr<Any>? = null

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
