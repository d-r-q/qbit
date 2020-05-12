package qbit.reflection

import qbit.api.model.Attr
import kotlin.reflect.*

expect fun findProperties(type: KClass<*>): List<KCallable<*>>

expect fun <T : Any> findPrimaryConstructor(type: KClass<T>): KFunction<T>

fun setableProps(type: KClass<*>): List<KMutableProperty1<Any, Any>> {
    return findProperties(type).filterIsInstance<KMutableProperty1<Any, Any>>()
}

fun KClass<*>.propertyFor(attr: Attr<*>) =
        findProperties(this).firstOrNull { attr.name.endsWith(it.name) }

@Suppress("UNCHECKED_CAST")
fun <T : Any> getListElementClass(type: KType) = type.arguments[0].type!!.classifier as KClass<T>
