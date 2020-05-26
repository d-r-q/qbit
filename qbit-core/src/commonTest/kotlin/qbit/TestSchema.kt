package qbit

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.factoring.AttrName
import qbit.schema.schema
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
    entity(Scientist::class)
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

object TheSimplestEntities : EntitySchema(TheSimplestEntity::class) {

    val scalar by attr(TheSimplestEntity::scalar)

}

object EntityWithByteArrays : EntitySchema(EntityWithByteArray::class) {

    val byteArray by attr(EntityWithByteArray::byteArray)

}

object EntityWithListOfBytess : EntitySchema(EntityWithListOfBytes::class) {

    val bytes by attr(EntityWithListOfBytes::bytes)

}

object EntityWithListOfByteArrays : EntitySchema(EntityWithListOfByteArray::class) {

    val byteArrays by attr(EntityWithListOfByteArray::byteArrays)
}

object EntityWithListOfStringss : EntitySchema(EntityWithListOfString::class) {

    val strings by attr(EntityWithListOfString::strings)
}

object Scientists : EntitySchema(Scientist::class) {

    val extId by attr(Scientist::externalId)
    val name by attr(Scientist::name)
    val nicks by attr(Scientist::nicks)
    val reviewer by attr(Scientist::reviewer)
    val country by attr(Scientist::country)
}

object Countries : EntitySchema(Country::class) {

    val name by attr(Country::name)
    val population by attr(Country::population)

}

object Regions : EntitySchema(Region::class) {

    val name by attr(Region::name)
    val country by attr(Region::country)

}

object Cities : EntitySchema(City::class) {

    val name by attr(City::name)
    val region by attr(City::region)

}

object Papers : EntitySchema(Paper::class) {

    val name by attr(Paper::name)

}

object ResearchGroups : EntitySchema(ResearchGroup::class) {

    val members by attr(ResearchGroup::members)

}

object Bombs : EntitySchema(Bomb::class) {

    val bool by attr(Bomb::bool)

}

object NullableScalars : EntitySchema(NullableScalar::class) {

    val scalar by attr(NullableScalar::scalar)
    val placeholder by attr(NullableScalar::placeholder)

}

object NullableLists : EntitySchema(NullableList::class) {

    val lst by attr(NullableList::lst)
    val placeholder by attr(NullableList::placeholder)

}

object NullableRefs : EntitySchema(NullableRef::class) {

    val ref by attr(NullableRef::ref)
    val placeholder by attr(NullableRef::placeholder)

}

object IntEntities : EntitySchema(IntEntity::class) {

    val int by attr(IntEntity::int)

}

object GidEntities : EntitySchema(GidEntity::class) {

    val bool by attr(GidEntity::bool)

}

object ParentToChildrenTreeEntities : EntitySchema(ParentToChildrenTreeEntity::class) {

    val name by attr(ParentToChildrenTreeEntity::name)
    val children by attr(ParentToChildrenTreeEntity::children)

}

fun KClass<*>.attrName(prop: KProperty1<*, *>): String {
    val className = this.simpleName
        ?: throw IllegalArgumentException("qbit entities should be represented but usual classes with simpleName, got: $this")
    return AttrName(className, prop.name).asString()
}
