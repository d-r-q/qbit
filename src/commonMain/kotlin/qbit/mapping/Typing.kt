package qbit.mapping

import qbit.QBitException
import qbit.findPrimaryConstructor
import qbit.isId
import qbit.model.DataType
import qbit.model.Gid
import qbit.model.StoredEntity
import qbit.setableProps
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter


class Typing<T : Any>(internal val root: StoredEntity, internal val query: Query<T>, internal val type: KClass<T>) {

    internal val entities = HashMap<Gid, StoredEntity>()
    internal val instances = HashMap<Gid, Any>()

    init {
        fun body(root: StoredEntity) {
            if (entities[root.gid] != null) {
                return
            } else {
                entities[root.gid] = root
            }
            root.entries
                    .filter { DataType.ofCode(it.attr.type)!!.ref() }
                    .filter { query.shouldFetch(it.attr) }
                    .forEach {
                        root.pull(it.value as Gid)?.let { e -> body(e) }
                    }
        }

        body(root)
    }

    fun <R : Any> instantiate(e: StoredEntity, type: KClass<R>): R {
        if (instances[e.gid] != null) {
            return instances[e.gid] as R
        }
        val constr = findPrimaryConstructor(type)
        var params: Map<KParameter, Any?> = constr.parameters.map { it to null }.toMap()

        params = params.map { (param, _) ->
            if (!param.type.isMarkedNullable || param.type.classifier in valueTypes) {
                if (param.isId())  {
                    val id = when {
                        param.type.classifier == Long::class -> e.gid.value()
                        param.type.classifier == Gid::class -> e.gid
                        else -> throw QBitException("Unexpected id type: ${param.type.classifier}")
                    }
                    return@map param to id
                }
                val attr = e.keys.firstOrNull { it.name.endsWith(param.name!!) }
                        ?: throw QBitException("Could not map $e to $type. Missed property: ${param.name}. Did you add this property?")
                when {
                    param.type.classifier in valueTypes -> param to e[attr]
                    param.type.classifier == List::class -> {
                        when (param.type.arguments[0].type!!.classifier as KClass<*>) {
                            in valueTypes -> param to e[attr]
                            else -> param to ((e[attr] as List<Gid>).map {
                                instantiate(entities[it]!!, param.type.arguments[0].type!!.classifier as KClass<Any>)
                            }) as Pair<KParameter, Any>
                        }
                    }
                    else -> {
                        param to instantiate(entities[e[attr] as Gid]!!,
                                param.type.classifier as KClass<Any>)
                    }
                }
            } else {
                param to null
            }
        }.toMap()
        val instance = constr.callBy(params)
        instances[e.gid] = instance
        val setters = setableProps(type)
        setters.forEach {
            val attr = e.keys.firstOrNull { attr -> attr.name.endsWith(it.name) }
            if (attr != null && query.shouldFetch(attr)) {
                val gid = e[attr] as Gid
                (it as KMutableProperty1<Any, Any>).set(instance, instantiate(entities[gid]!!, it.returnType.classifier as KClass<*>))
            }
        }
        return instance
    }
}