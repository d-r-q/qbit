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

data class Country(val id: Long?, val name: String, val population: Int?)

data class User(val id: Long?, val externalId: Int, val name: String, val nicks: List<String>, val country: Country, var reviewer: User? = null) {

    fun toFacts() =
            destruct(this, schemaMap::get, EmptyIterator)

    override fun toString(): String {
        return "User($id, $name)"
    }
}

data class Region(val id: Long?, val name: String, val country: Country)

data class Paper(val id: Long, val name: String, val editor: User?)

val testSchema = schema {
    entity(User::class) {
        unique(it::externalId)
    }
    entity(Country::class) {
        unique(it::name)
    }
    entity(Region::class)
    entity(Paper::class)
}

private val gids = Gid(2, 0).nextGids()
val schemaMap: Map<String, Attr<Any>> = testSchema
        .map { it.name to it.id(gids.next()) }
        .toMap()

object Users {

    val extId: Attr<Int> = schemaMap.getValue(User::class.attrName(User::externalId)) as Attr<Int>
    val name: Attr<String> = schemaMap.getValue(User::class.attrName(User::name)) as Attr<String>
    val nicks: Attr<List<String>> = schemaMap.getValue(User::class.attrName(User::nicks)) as Attr<List<String>>
    val reviewer = schemaMap.getValue(User::class.attrName(User::reviewer))
    val country = schemaMap.getValue(User::class.attrName(User::country))
}

object Countries {

    val name = schemaMap.getValue(Country::class.attrName(Country::name))
    val population = schemaMap.getValue(Country::class.attrName(Country::population))

}

object Regions {

    val name = schemaMap.getValue(Region::class.attrName(Region::name))
    val country = schemaMap.getValue(Region::class.attrName(Region::country))

}

object Papers {

    val name = schemaMap.getValue(Paper::class.attrName(Paper::name))
    val editor = schemaMap.getValue(Paper::class.attrName(Paper::editor))

}

val uk = Country(gids.next().value(), "United Kingdom", 1)
val tw = Country(gids.next().value(), "Taiwan", 2)
val us = Country(gids.next().value(), "USA", 3)

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