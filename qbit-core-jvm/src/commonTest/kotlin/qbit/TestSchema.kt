package qbit

import kotlinx.serialization.modules.plus
import qbit.api.db.Conn
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.factoring.serializatoin.KSFactorizer
import qbit.platform.collections.EmptyIterator
import qbit.schema.schema
import qbit.spi.Storage
import qbit.storage.MemStorage
import qbit.test.model.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

val testSchemaFactorizer =
    KSFactorizer(qbitSerialModule + testsSerialModule)

fun Scientist.toFacts() =
    testSchemaFactorizer.factor(this, schemaMap::get, EmptyIterator)


val testSchema = schema(testsSerialModule) {
    entity(Scientist::class) {
        uniqueInt(Scientist::externalId)
    }
    entity(Country::class) {
        uniqueString(Country::name)
    }
    entity(Region::class)
    entity(Paper::class)
    entity(ResearchGroup::class)
    entity(City::class)
    entity(Bomb::class)
    entity(NullableScalar::class)
    entity(NullableList::class)
    entity(NullableRef::class)
    entity(IntEntity::class)
    entity(EntityWithoutAttrs::class)
    entity(NullableScalarWithoutPlaceholder::class)
    entity(MUser::class)
    entity(TheSimplestEntity::class)
}

private val gids = Gid(2, 0).nextGids()
val schemaMap: Map<String, Attr<Any>> = testSchema
        .map { it.name to it.id(gids.next()) }
        .toMap()

object Scientists {

    val extId = schemaMap.getValue(Scientist::class.attrName(Scientist::externalId))
    val name = schemaMap.getValue(Scientist::class.attrName(Scientist::name))
    val nicks = schemaMap.getValue(Scientist::class.attrName(Scientist::nicks))
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

object ResearchGroups {

    val members = schemaMap.getValue(ResearchGroup::class.attrName(ResearchGroup::members))

}

object Bombs {

    val bool = schemaMap.getValue(Bomb::class.attrName(Bomb::bool))

}

object NullableLists {

    val placeholder = schemaMap.getValue(NullableList::class.attrName(NullableList::placeholder))

}

object IntEntities {

    val int = schemaMap.getValue(IntEntity::class.attrName(IntEntity::int))

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

suspend fun setupTestSchema(storage: Storage = MemStorage()): Conn {
    val conn = qbit(storage, testsSerialModule)
    testSchema.forEach {
        conn.persist(it)
    }
    return conn
}

suspend fun setupTestData(storage: Storage = MemStorage()): Conn {
    return with(setupTestSchema(storage)) {
        listOf(eCodd, pChen, mStonebreaker, eBrewer, uk, tw, us, ru, nsk).forEach {
            persist(it)
        }
        this
    }
}

fun KClass<*>.attrName(prop: KProperty1<*, *>): String =
   this.simpleName!! + "/" + prop.name
