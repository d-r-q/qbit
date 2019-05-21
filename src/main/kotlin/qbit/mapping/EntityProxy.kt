package qbit.mapping

import qbit.*
import java.lang.IllegalStateException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

interface EntityHolder {

    fun entity(): Entity

    fun eid(): EID?

}

private class EntityProxy<T>(private val entity: Entity, private val clazz: Class<T>) : InvocationHandler {

    private val method2attr = entity.keys
            .map { it.name.name to it }
            .toMap()

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        return if (method.name == "entity" && method.returnType == Entity::class.java) {
            return entity
        } else if (method.name == "eid" && method.returnType == EID::class.java) {
            return (entity as? IdentifiedEntity)?.eid
        } else if (entity.javaClass.methods.contains(method)) {
            if (args == null || args.isEmpty()) {
                method.invoke(entity)
            } else {
                method.invoke(entity, args)
            }
        } else {
            val name = method.name
            if (args == null && name in method2attr) {
                entity[method2attr.getValue(name)]
            } else if (args?.size == 1 && name in method2attr) {
                proxy(entity.set(method2attr.getValue(name), args[0]), clazz)
            } else {
                throw IllegalStateException("Method $method could not be implemented")
            }
        }
    }

}

inline fun <reified T> proxy(entity: Entity): T {
    return proxy(entity, T::class.java)
}

fun <T> proxy(entity: Entity, clazz: Class<T>): T {
    @Suppress("UNCHECKED_CAST")
    return Proxy.newProxyInstance(
            EntityProxy::class.java.classLoader,
            arrayOf<Class<*>>(clazz),
            EntityProxy(entity, clazz)) as T
}

inline fun <reified T> Db.pullAs(eid: EID): T? = this.pull(eid)?.let { proxy(it, T::class.java) }

inline fun <reified T> Db.queryAs(vararg preds: QueryPred): Sequence<T> = this.query(*preds).map { proxy(it, T::class.java) }
