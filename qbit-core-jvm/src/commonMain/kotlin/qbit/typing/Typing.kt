package qbit.typing

import qbit.api.QBitException
import qbit.api.db.Query
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.DataType
import qbit.api.model.Entity
import qbit.api.model.QList
import qbit.api.model.StoredEntity
import qbit.factoring.valueTypes
import qbit.reflection.findPrimaryConstructor
import qbit.reflection.getListElementClass
import qbit.reflection.isId
import qbit.reflection.propertyFor
import qbit.reflection.setableProps
import kotlin.reflect.KClass
import kotlin.reflect.KParameter


class Typing<T : Any>(internal val root: StoredEntity, internal val query: Query, internal val type: KClass<T>) {

    internal val entities = HashMap<Gid, StoredEntity>()
    private val instances = HashMap<Gid, Any>()
    private var fetchedAttrs: Set<Attr<Any>> = HashSet()

    init {

        fun <T : Any> body(root: StoredEntity, type: KClass<T>, query: Query) {
            if (entities[root.gid] != null) {
                return
            } else {
                entities[root.gid] = root
            }
            val fetchedAttrValues = root.entries
                    .filter { DataType.ofCode(it.attr.type)!!.ref() }
                    .filter { query.shouldFetch(it.attr) }
            fetchedAttrs = fetchedAttrs + fetchedAttrValues.map { it.attr }.toSet()
            fetchedAttrValues
                    .forEach {

                        if (it.attr.list) {
                            val subType = getListElementClass<T>(type.propertyFor(it.attr)!!.returnType)
                            val subquery = query.subquery(subType)
                            @Suppress("UNCHECKED_CAST")
                            (it.value as List<Gid>).forEach { gid ->
                                root.pull(gid)?.let { e -> body(e, subType, subquery) }
                            }
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            val subType = type.propertyFor(it.attr)!!.returnType.classifier as KClass<Any>
                            val subquery = query.subquery(subType)
                            root.pull(it.value as Gid)?.let { e -> body(e, subType, subquery) }
                        }
                    }
        }

        body(root, type, query)
    }

    fun <R : Any> instantiate(e: StoredEntity, type: KClass<R>): R {
        if (instances[e.gid] != null) {
            @Suppress("UNCHECKED_CAST")
            return instances[e.gid] as R
        }
        val constr = findPrimaryConstructor(type)
        var params: Map<KParameter, Any?> = constr.parameters.map { it to null }.toMap()
        val setters = setableProps(type)
        val setablePropNames = setters.map { it.name }

        params = params.map { (param, _) ->
            val attr = e.keys.firstOrNull { it.name.endsWith(param.name!!) }
            if (!param.type.isMarkedNullable || param.type.classifier in valueTypes ||
                    (param.type.classifier == List::class && (param.type.arguments[0].type!!.classifier as KClass<*>) in valueTypes) ||
                    (param.name !in setablePropNames && attr in fetchedAttrs)) {
                if (param.isId())  {
                    val id = when {
                        param.type.classifier == Long::class -> e.gid.value()
                        param.type.classifier == Gid::class -> e.gid
                        else -> throw QBitException("Unexpected id type: ${param.type.classifier}")
                    }
                    return@map param to id
                }
                if (attr == null) {
                    if (!param.type.isMarkedNullable) {
                        throw QBitException("Could not map $e to $type. Missed property: ${param.name}. Did you add or made not nullable this property?")
                    } else {
                        return@map param to null
                    }
                }
                when {
                    param.type.classifier in valueTypes -> param to e[attr]
                    param.type.classifier == List::class -> {
                        when (param.type.arguments[0].type!!.classifier as KClass<*>) {
                            in valueTypes -> param to e[attr]
                            else -> (param to e.getGidList(attr).map {
                                instantiate(entities[it]!!, getListElementClass<T>(param.type))
                            })
                        }
                    }
                    else -> {
                        val referedEntity = entities[e[attr] as Gid] ?: throw QBitException("There is no entity for gid: ${e[attr]}, attr: $attr")
                        @Suppress("UNCHECKED_CAST")
                        val instance = instantiate(referedEntity, param.type.classifier as KClass<Any>)
                        param to instance
                    }
                }
            } else {
                param to null
            }
        }.toMap()
        val instance = constr.callBy(params)
        instances[e.gid] = instance
        setters.forEach { prop ->
            val attr = e.keys.firstOrNull { attr -> attr.name.endsWith(prop.name) }
            if (attr != null) {
                val propType = DataType.ofCode(attr.type)
                if (propType?.value() == true) {
                    prop.set(instance, e[attr])
                } else if (propType?.isList() == true && (propType as QList<Any>).value()) {
                    prop.set(instance, e[attr])
                } else if (query.shouldFetch(attr)) {
                    if (propType?.isList() != true) {
                        val gid = e[attr] as Gid
                        prop.set(instance, instantiate(entities[gid]!!, prop.returnType.classifier as KClass<*>))
                    } else {
                        val instances = e.getGidList(attr).map { gid -> instantiate(entities[gid]!!, prop.returnType.arguments[0].type!!.classifier as KClass<*>) }
                        prop.set(instance, instances)
                    }
                }
            }
        }
        return instance
    }

    @Suppress("UNCHECKED_CAST")
    private fun Entity.getGidList(attr: Attr<*>) = this[attr] as List<Gid>
}