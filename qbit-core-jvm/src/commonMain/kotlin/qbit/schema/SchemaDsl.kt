package qbit.schema

import qbit.api.model.Attr
import qbit.api.model.QRef
import qbit.factoring.attrName
import qbit.factoring.collectionTypes
import qbit.factoring.types
import qbit.factoring.valueTypes
import qbit.reflection.default
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

fun schema(body: SchemaBuilder.() -> Unit): List<Attr<Any>> {
    val scb = SchemaBuilder()
    scb.body()
    return scb.attrs
}

class SchemaBuilder {

    internal val attrs: MutableList<Attr<Any>> = ArrayList()

    fun <T : Any> entity(type: KClass<T>, body: EntityBuilder.(T) -> Unit = {}) {
        val eb = EntityBuilder(type)
        eb.body(default(type))
        attrs.addAll(schemaFor(type, eb.uniqueProps))
    }

}

class EntityBuilder(private val type: KClass<*>) {

    internal val uniqueProps = HashSet<String>()

    fun uniqueInt(prop: KProperty0<Int>) {
        uniqueProps.add(type.attrName(prop))
    }

    fun uniqueString(prop: KProperty0<String>) {
        uniqueProps.add(type.attrName(prop))
    }

}

private fun schemaFor(type: KClass<*>, unique: Set<String>): Collection<Attr<Any>> {
    val getters = type.members.filterIsInstance<KProperty1<*, *>>()
    val (ids, attrs) = getters.partition { it.name == "id" && it.returnType.classifier == Long::class }
    val id = ids.firstOrNull()
    id ?: throw IllegalArgumentException("Type $type does not contains id: Long property")
    return attrs.map {
        when (it.returnType.classifier) {
            in valueTypes -> Attr(
                null,
                type.attrName(it),
                types.getValue(it.returnType.classifier as KClass<*>).code,
                type.attrName(it) in unique,
                false
            )
            in collectionTypes -> {
                when (val valueType = it.returnType.arguments[0].type!!.classifier as KClass<*>) {
                    in valueTypes -> Attr(
                        null,
                        type.attrName(it),
                        types.getValue(valueType).list().code,
                        type.attrName(it) in unique,
                        true
                    )
                    else -> Attr<Any>(
                        null,
                        type.attrName(it),
                        QRef.list().code,
                        type.attrName(it) in unique,
                        true
                    )
                }
            }
            else -> {
                Attr(
                    null,
                    type.attrName(it),
                    QRef.code,
                    type.attrName(it) in unique,
                    false
                )
            }
        }
    }
}
