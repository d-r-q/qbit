package qbit

import qbit.model.*

fun dbOf(vararg entities: RoEntity<*>): Db {
    val eids = EID(0, 0).nextEids()
    val facts = entities.flatMap { it.toFacts(eids.next()) }
    return IndexDb(Index(facts.groupBy { it.eid }.map { it.key to it.value }), Hash(byteArrayOf()))
}

object emptyDb : Db {

    override val hash: Hash = nullHash

    override fun pull(eid: EID): StoredEntity? = null

    override fun query(vararg preds: QueryPred): Sequence<StoredEntity> = emptySequence()

    override fun attr(attr: String): Attr<Any>? = null

}