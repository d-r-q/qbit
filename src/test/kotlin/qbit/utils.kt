package qbit

fun dbOf(vararg entities: Entitiable): Db {
    val eids = EID(0, 0).nextEids()
    val facts = entities.flatMap { it.toFacts(eids.next()) }
    return IndexDb(Index(facts))
}