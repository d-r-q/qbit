package qbit

import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.serializersModuleOf
import qbit.api.model.Attr
import qbit.api.db.Conn
import qbit.platform.collections.EmptyIterator
import qbit.factorization.attrName
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.system.Instance
import qbit.factorization.KSFactorization
import qbit.api.model.impl.QTombstone
import qbit.storage.MemStorage
import qbit.spi.Storage
import qbit.schema.schema
import qbit.test.model.*
import qbit.test.model.FakeSerializer
import kotlin.reflect.KClass

val serializersMap: Map<KClass<*>, KSerializer<*>> = mapOf<KClass<*>, KSerializer<*>>(
    Scientist::class to Scientist.serializer(),
    Attr::class to Attr.serializer(FakeSerializer<Any>()),
    Region::class to Region.serializer(),
    Country::class to Country.serializer(),
    Instance::class to Instance.serializer(),
    ResearchGroup::class to ResearchGroup.serializer(),
    IntEntity::class to IntEntity.serializer(),
    Bomb::class to Bomb.serializer(),
    QTombstone::class to QTombstone.serializer()
)
val testSchemaFactorization = KSFactorization(serializersModuleOf(serializersMap))

fun Scientist.toFacts() =
    testSchemaFactorization.ksDestruct(this, schemaMap::get, EmptyIterator)


data class City(val id: Long?, val name: String, val region: Region)

data class Paper(val id: Long?, val name: String, val editor: Scientist?)

data class NullableScalarWithoutPlaceholder(val id: Long?, val scalar: Int?)

data class NullableRef(val id: Long?, val ref: IntEntity?, val placeholder: Long)

data class EntityWithoutAttrs(val id: Long?)

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
    entity(Bomb::class)
    entity(ListOfNullables::class)
    entity(NullableScalar::class)
    entity(NullableList::class)
    entity(NullableRef::class)
    entity(IntEntity::class)
    entity(EntityWithoutAttrs::class)
    entity(NullableScalarWithoutPlaceholder::class)
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

object Cities {

    val name = schemaMap.getValue(City::class.attrName(City::name))
    val region = schemaMap.getValue(City::class.attrName(City::region))

}

object Papers {

    val name = schemaMap.getValue(Paper::class.attrName(Paper::name))

}

object ResearchGroups {

    val members = schemaMap.getValue(ResearchGroup::class.attrName(ResearchGroup::members))

}

object Bombs {

    val bool = schemaMap.getValue(Bomb::class.attrName(Bomb::bool))

}

object NullableScalars {

    val scalar = schemaMap.getValue(NullableScalar::class.attrName(NullableScalar::scalar))
    val placeholder = schemaMap.getValue(NullableScalar::class.attrName(NullableScalar::placeholder))

}

object NullableLists {

    val lst = schemaMap.getValue(NullableList::class.attrName(NullableList::lst))
    val placeholder = schemaMap.getValue(NullableList::class.attrName(NullableList::placeholder))

}

object NullableRefs {

    val ref = schemaMap.getValue(NullableRef::class.attrName(NullableRef::ref))
    val placeholder = schemaMap.getValue(NullableRef::class.attrName(NullableRef::placeholder))

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

fun setupTestSchema(storage: Storage = MemStorage()): Conn {
    val conn = qbit(storage, testSchemaFactorization::ksDestruct)
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