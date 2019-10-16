package qbit

import qbit.Attrs.unique
import qbit.mapping.attrName
import qbit.mapping.destruct
import qbit.mapping.schema
import qbit.model.Attr
import qbit.model.Gid
import qbit.storage.MemStorage
import qbit.storage.Storage
import qbit.trx.Conn
import qbit.trx.EmptyIterator
import qbit.trx.qbit

data class Country(val id: Long?, val name: String, val population: Int?)

data class Scientist(val id: Long?, val externalId: Int, val name: String, val nicks: List<String>, val country: Country, var reviewer: Scientist? = null) {

    fun toFacts() =
            destruct(this, schemaMap::get, EmptyIterator)

    override fun toString(): String {
        return "User($id, $name)"
    }
}

data class Region(val id: Long?, val name: String, val country: Country)

data class City(val id: Long?, val name: String, val region: Region)

data class Paper(val id: Long?, val name: String, val editor: Scientist?)

data class ResearchGroup(val id: Long?, val members: List<Scientist>)

val testSchema = schema {
    entity(Scientist::class) {
        uniqueInt(it::externalId)
    }
    entity(Country::class) {
        uniqueString(it::name)
    }
    entity(Region::class)
    entity(Paper::class)
    entity(ResearchGroup::class)
    entity(City::class)
}

private val gids = Gid(2, 0).nextGids()
val schemaMap: Map<String, Attr<Any>> = testSchema
        .map { it.name to it.id(gids.next()) }
        .toMap()

object Scientists {

    val extId: Attr<Int> = schemaMap.getValue(Scientist::class.attrName(Scientist::externalId)) as Attr<Int>
    val name: Attr<String> = schemaMap.getValue(Scientist::class.attrName(Scientist::name)) as Attr<String>
    val nicks: Attr<List<String>> = schemaMap.getValue(Scientist::class.attrName(Scientist::nicks)) as Attr<List<String>>
    val reviewer = schemaMap.getValue(Scientist::class.attrName(Scientist::reviewer))
    val country = schemaMap.getValue(Scientist::class.attrName(Scientist::country))
}

object Countries {

    val name = schemaMap.getValue(Country::class.attrName(Country::name))
    val population = schemaMap.getValue(Country::class.attrName(Country::population))

}

object Regions {

    val name = schemaMap.getValue(Region::class.attrName(Region::name))
    val country = schemaMap.getValue(Region::class.attrName(Region::country))

}

object Cities {

    val name = schemaMap.getValue(City::class.attrName(City::name))
    val region = schemaMap.getValue(City::class.attrName(City::region))

}

object Papers {

    val name = schemaMap.getValue(Paper::class.attrName(Paper::name))
    val editor = schemaMap.getValue(Paper::class.attrName(Paper::editor))

}

object ResearchGroups {

    val members = schemaMap.getValue(ResearchGroup::class.attrName(ResearchGroup::members))

}

val uk = Country(gids.next().value(), "United Kingdom", 63_000_000)
val tw = Country(gids.next().value(), "Taiwan", 23_000_000)
val us = Country(gids.next().value(), "USA", 328_000_000)
val ru = Country(gids.next().value(), "Russia", 146_000_000)
val nsk = Region(gids.next().value(), "Novosibirskaya obl.", ru)

val eCodd = Scientist(gids.next().value(), 1, "Edgar Codd", listOf("mathematician", "tabulator"), uk)
val pChen = Scientist(gids.next().value(), 2, "Peter Chen", listOf("unificator"), tw)
val mStonebreaker = Scientist(gids.next().value(), 3, "Michael Stonebreaker", listOf("The DBMS researcher"), us)
val eBrewer = Scientist(gids.next().value(), 4, "Eric Brewer", listOf("Big Data"), us)

fun setupTestSchema(storage: Storage = MemStorage()): Conn {
    val conn = qbit(storage)
    testSchema.forEach {
        conn.persist(it)
    }
    return conn
}

fun setupTestData(storage: Storage = MemStorage()): Conn {
    return with(setupTestSchema(storage)) {
        listOf(eCodd, pChen, mStonebreaker, eBrewer, uk, tw, us, ru, nsk).forEach {
            persist(it)
        }
        this
    }
}