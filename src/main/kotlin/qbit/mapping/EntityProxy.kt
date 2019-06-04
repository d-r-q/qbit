package qbit.mapping

import qbit.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy

interface EntityHolder {

    fun entity(): Entity

    fun eid(): EID?

}

private class EntityProxy<T>(private var entity: Entity, private val clazz: Class<T>) : InvocationHandler {

    private val method2attr = entity.keys
            .map { it.name.name to it }
            .toMap()

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        return if (method.name == "entity" && method.returnType == Entity::class.java) {
            return entity
        } else if (method.name == "eid" && method.returnType == EID::class.java) {
            return (entity as? IdentifiedEntity)?.eid
        } else {
            val name = method.name.let {
                if (it.startsWith("set") || it.startsWith("get")) {
                    it[3].toLowerCase() + it.substring(4)
                } else {
                    it
                }
            }
            if (args == null) {
                method2attr[name]?.let {
                    when (val res = entity.getO(it)) {
                        is Entity -> proxy(res, method.returnType)
                        is List<*> -> res.map { item ->
                            if (item is Entity) {
                                proxy(item, (method.genericReturnType as ParameterizedType).actualTypeArguments[0] as Class<*>)
                            } else {
                                item
                            }
                        }
                        else -> res
                    }
                }
            } else if (args.size == 1 && name in method2attr) {
                @Suppress("UNCHECKED_CAST")
                val toStore = when {
                    args[0] is EntityHolder -> (args[0] as EntityHolder).entity()
                    args[0] is List<*> && (args[0] as List<*>).size > 0 &&
                            (args[0] as List<*>)[0] is EntityHolder ->
                        (args[0] as List<EntityHolder>).map { it.entity() }
                    else -> args[0]
                }
                val attr = method2attr.getValue(name)
                val newEntity = entity.set(attr, toStore)
                if (method.returnType == Void.TYPE) {
                    entity = newEntity
                    Unit
                } else {
                    proxy(newEntity, clazz)
                }
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
