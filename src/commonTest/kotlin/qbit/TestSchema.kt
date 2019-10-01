package qbit

import qbit.mapping.attrName
import qbit.mapping.destruct
import qbit.mapping.schema
import qbit.model.Attr
import qbit.model.Gid
import qbit.storage.MemStorage
import qbit.trx.Conn
import qbit.trx.EmptyIterator
import qbit.trx.qbit

data class Country(val id: Long?, val name: String)

data class User(val id: Long?, val externalId: Int, val name: String, val nicks: List<String>, val country: Country) {

    fun toFacts() =
            destruct(this, schemaMap::get, EmptyIterator)

}

val testSchema = schema {
    entity(User::class) {
        unique(it::externalId)
    }
    entity(Country::class) {
        unique(it::name)
    }
}

private val gids = Gid(2, 0).nextGids()
val schemaMap: Map<String, Attr<Any>> = testSchema
        .map { it.name to it.id(gids.next()) }
        .toMap()

object Users {

    val extId = schemaMap.getValue(User::class.attrName(User::externalId))
    val name = schemaMap.getValue(User::class.attrName(User::name))
    val nicks = schemaMap.getValue(User::class.attrName(User::nicks))

}

object Countries {

    val name = schemaMap.getValue(Country::class.attrName(Country::name))

}

val uk = Country(gids.next().value(), "United Kingdom")
val tw = Country(gids.next().value(), "Taiwan")
val us = Country(gids.next().value(), "USA")

val eCodd = User(gids.next().value(), 1, "Edgar Codd", listOf("mathematician", "tabulator"), uk)
val pChen = User(gids.next().value(), 2, "Peter Chen", listOf("unificator"), tw)
val mStonebreaker = User(gids.next().value(), 3, "Michael Stonebreaker", listOf("The DBMS researcher"), us)
val eBrewer = User(gids.next().value(), 4, "Eric Brewer", listOf("Big Data"), us)

fun <T> test(m: Map<T, String>) {}

fun setupTestSchema(): Conn {
    val map = hashMapOf<Number, String>()
    test(map)
    map.put(1, "")
    val conn = qbit(MemStorage())
    testSchema.forEach {
        conn.persist(it)
    }
    return conn
}

fun setupTestData(): Conn {
    return with(setupTestSchema()) {
        listOf(eCodd, pChen, mStonebreaker, eBrewer).forEach {
            persist(it)
        }
        this
    }
}