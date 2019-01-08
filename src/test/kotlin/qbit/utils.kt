package qbit

import qbit.model.EID
import qbit.model.Entitiable
import qbit.model.Entity
import qbit.model.toFacts
import qbit.model.Attr

fun dbOf(vararg entities: Any): Db {
    val eids = EID(0, 0).nextEids()
    val facts = entities.map { it as? Entitiable ?: Entity(it as Attr<*>) }. flatMap { it.toFacts(eids.next()) }
    return IndexDb(Index(facts))
}