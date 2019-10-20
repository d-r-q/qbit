package qbit.factorization

import qbit.model.*
import qbit.model.impl.QBitException
import qbit.platform.BigDecimal
import qbit.platform.IdentityHashMap
import qbit.platform.Instant
import qbit.platform.ZonedDateTime
import qbit.reflection.findProperties
import kotlin.collections.Collection
import kotlin.collections.HashMap
import kotlin.collections.Iterator
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.collections.arrayListOf
import kotlin.collections.contains
import kotlin.collections.distinct
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.filterIsInstance
import kotlin.collections.firstOrNull
import kotlin.collections.flatMap
import kotlin.collections.forEach
import kotlin.collections.getValue
import kotlin.collections.groupBy
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mapOf
import kotlin.collections.partition
import kotlin.collections.set
import kotlin.collections.setOf
import kotlin.collections.sortedBy
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1


val collectionTypes = setOf(List::class)

val types: Map<KClass<*>, DataType<*>> = mapOf(
        Boolean::class to QBoolean,
        Byte::class to QByte,
        Int::class to QInt,
        Long::class to QLong,
        BigDecimal::class to QDecimal,
        Instant::class to QInstant,
        ZonedDateTime::class to QZonedDateTime,
        String::class to QString,
        ByteArray::class to QBytes,
        Gid::class to QGid,
        Any::class to QRef
)

val valueTypes = types.values
        .filter { it.value() }
        .map { it.typeClass() }

fun identifyEntityGraph(root: Any, eids: Iterator<Gid>, idMap: IdentityHashMap<Any, Gid>) {
    if (idMap[root] != null) {
        return
    }
    validate(root::class, findProperties(root::class))

    val idProp = findGidProp(root)

    when (val id = idProp.call(root)) {
        null -> idMap[root] = eids.next()
        is Gid -> idMap[root] = id
        is Long -> idMap[root] = Gid(id)
    }

    root::class.members
            .filterIsInstance<KProperty1<*, *>>()
            .forEach {
                val prop = root::class.members.firstOrNull { m -> m.name == it.name }!!
                if (prop == idProp) {
                    return@forEach
                }
                val value = prop.call(root) ?: return@forEach

                when (it.returnType.classifier) {
                    in valueTypes -> return@forEach
                    in collectionTypes -> {
                        @Suppress("UNCHECKED_CAST")
                        val list = value as List<Any>?
                        if (isListOfVals(list)) {
                            return@forEach
                        } else {
                            list!!.forEach { item ->
                                identifyEntityGraph(item, eids, idMap)
                            }
                        }
                    }
                    else -> {
                        identifyEntityGraph(value, eids, idMap)
                    }
                }
            }

}

internal fun findGidProp(root: Any): KCallable<*> =
        root::class.members
                .firstOrNull {
                    it is KProperty1<*, *> && it.name == "id" && (it.returnType.classifier == Long::class || it.returnType.classifier == Gid::class)
                }
                ?: throw IllegalArgumentException("Entity $root does not contains `id: (Long|Gid)` property")

fun destruct(e: Any, schema: (String) -> Attr<*>?, gids: Iterator<Gid>): EntityGraphFactorization {
    if (e is Tombstone) {
        val entityFacts = IdentityHashMap<Any, List<Eav>>().apply {
            this[e] = e.toFacts()
        }
        return EntityGraphFactorization(entityFacts)
    }

    val res = IdentityHashMap<Any, List<Eav>>()
    val idMap = IdentityHashMap<Any, Gid>()
    val entities = HashMap<Gid, Any>()

    fun body(e: Any): List<Eav> {
        if (res.containsKey(e)) {
            return res[e]!!
        } else {
            res[e] = emptyList()
        }

        val getters = e::class.members
                .filterIsInstance<KProperty1<*, *>>()
                .sortedBy { it.name }

        val (_, attrs) = getters.partition { it.name == "id" && it.returnType.classifier == Long::class || it.returnType.classifier == Gid::class }
        val gid = idMap[e]!! // ids existence has been checked while identification
        if (entities[gid] != null) {
            // different instance with the same state
            return emptyList()
        }
        val facts: List<Eav> = attrs.flatMap {
            val attr: Attr<*> = schema(e::class.attrName(it))
                    ?: throw QBitException("Attribute for property ${e::class.attrName(it)} isn't defined")
            when (it.returnType.classifier) {
                in valueTypes -> {
                    val value = it.call(e)
                    if (value != null) {
                        listOf(Eav(gid, attr.name, value))
                    } else {
                        emptyList()
                    }
                }
                in collectionTypes -> {
                    ((it.call(e) as List<*>?) ?: emptyList<Any>()).flatMap { v ->
                        when (v!!::class) {
                            in valueTypes -> listOf(Eav(gid, attr.name, v))
                            else -> {
                                body(v)
                                listOf(Eav(gid, attr.name, idMap[v]!!))
                            }
                        }
                    }
                }
                else -> {
                    val value = it.call(e) ?: return@flatMap emptyList<Eav>()

                    val fs = arrayListOf(Eav(gid, attr.name, idMap[value]!!))
                    body(value)
                    fs
                }
            }
        }
        res[e] = facts
        entities[gid] = e
        return facts
    }

    identifyEntityGraph(e, gids, idMap)
    idMap.entries
            .groupBy { it.value }
            .forEach { entityStates ->
                val distinctStates = entityStates.value.map { it.key }.distinct()
                if (distinctStates.size > 1) {
                    throw QBitException("Entity ${entityStates.key} has several different states to store: $distinctStates")
                }
            }

    body(e)

    return EntityGraphFactorization(res)
}

fun validate(type: KClass<*>, getters: List<KCallable<*>>) {
    val listsOfNullables = getters.filter { it.returnType.classifier == List::class && it.returnType.arguments[0].type!!.isMarkedNullable }
    if (listsOfNullables.isNotEmpty()) {
        val props = "${type.simpleName}.${listsOfNullables.joinToString(",", "(", ")") { it.name }}"
        throw QBitException("List of nullable elements is not supported. Properties: $props")
    }
}

fun schemaFor(type: KClass<*>, unique: Set<String>): Collection<Attr<Any>> {
    val getters = type.members.filterIsInstance<KProperty1<*, *>>()
    val (ids, attrs) = getters.partition { it.name == "id" && it.returnType.classifier == Long::class }
    val id = ids.firstOrNull()
    id ?: throw IllegalArgumentException("Type $type does not contains id: Long property")
    return attrs.map {
        when (it.returnType.classifier) {
            in valueTypes -> Attr(null, type.attrName(it), types.getValue(it.returnType.classifier as KClass<*>).code, type.attrName(it) in unique, false)
            in collectionTypes -> {
                when (val valueType = it.returnType.arguments[0].type!!.classifier as KClass<*>) {
                    in valueTypes -> Attr(null, type.attrName(it), types.getValue(valueType).list().code, type.attrName(it) in unique, true)
                    else -> Attr<Any>(null, type.attrName(it), QRef.list().code, type.attrName(it) in unique, true)
                }
            }
            else -> {
                Attr(null, type.attrName(it), QRef.code, type.attrName(it) in unique, false)
            }
        }
    }
}

fun KClass<*>.attrName(prop: KProperty1<*, *>): String =
        "." + this.qualifiedName!! + "/" + prop.name

fun KClass<*>.attrName(prop: KProperty0<*>): String =
        "." + this.qualifiedName!! + "/" + prop.name

