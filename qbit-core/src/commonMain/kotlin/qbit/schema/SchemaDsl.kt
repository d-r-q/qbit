package qbit.schema

import qbit.api.model.Attr
import qbit.factorization.attrName
import qbit.factorization.schemaFor
import qbit.reflection.default
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0

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