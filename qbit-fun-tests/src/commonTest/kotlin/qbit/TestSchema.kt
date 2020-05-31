package qbit

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import qbit.api.db.Conn
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.factoring.AttrName
import qbit.schema.schema
import qbit.spi.Storage
import qbit.storage.MemStorage
import qbit.test.model.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1


@Serializable
data class GidEntity(val id: Gid?, val bool: Boolean)

val internalTestsSerialModule = testsSerialModule + SerializersModule {
    contextual(GidEntity::class, GidEntity.serializer())
}

val testSchema = schema(internalTestsSerialModule) {
    entity(TheSimplestEntity::class)
    entity(Country::class)
    entity(Region::class)
    entity(City::class)
    entity(Paper::class)
    entity(Scientist::class) {
        uniqueInt(Scientist::externalId)
    }
    entity(NullableScalar::class)
    entity(NullableList::class)
    entity(NullableRef::class)
    entity(IntEntity::class)
    entity(ResearchGroup::class)
    entity(EntityWithByteArray::class)
    entity(EntityWithListOfBytes::class)
    entity(EntityWithListOfByteArray::class)
    entity(EntityWithListOfString::class)
    entity(MUser::class)
    entity(GidEntity::class)
    entity(ParentToChildrenTreeEntity::class)
    entity(Bomb::class)
}

private val gids = Gid(2, 0).nextGids()
val schemaMap: Map<String, Attr<Any>> = testSchema
    .map { it.name to it.id(gids.next()) }
    .toMap()

open class EntitySchema(val type: KClass<*>)

fun <R : Any?> attr(prop: KProperty1<*, R>) =
    AttrDelegate<Any, R>(prop)

class AttrDelegate<T : Any, R : Any?>(private val prop: KProperty1<*, R>) : ReadOnlyProperty<EntitySchema, Attr<T>> {

    override operator fun getValue(thisRef: EntitySchema, property: KProperty<*>): Attr<T> {
        return schemaMap.getValue(thisRef.type.attrName(prop)) as Attr<T>
    }

}

object Scientists : EntitySchema(Scientist::class) {

    val extId by attr(Scientist::externalId)
    val name by attr(Scientist::name)
}

object ResearchGroups : EntitySchema(ResearchGroup::class) {

    val members by attr(ResearchGroup::members)

}

object Bombs : EntitySchema(Bomb::class) {

    val bool by attr(Bomb::bool)

}

object NullableLists : EntitySchema(NullableList::class) {

    val placeholder by attr(NullableList::placeholder)

}

object IntEntities : EntitySchema(IntEntity::class) {

    val int by attr(IntEntity::int)

}

fun KClass<*>.attrName(prop: KProperty1<*, *>): String {
    val className = this.simpleName
        ?: throw IllegalArgumentException("qbit entities should be represented but usual classes with simpleName, got: $this")
    return AttrName(className, prop.name).asString()
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

