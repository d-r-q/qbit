package qbit.mapping

import qbit.*
import qbit.model.*
import qbit.platform.*
import kotlin.reflect.*


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

interface Query<T> {

    fun shouldFetch(attr: Attr<*>): Boolean

    fun <ST : Any> subquery(subType: KClass<ST>): Query<ST>

}

class EagerQuery<T> : Query<T> {

    override fun shouldFetch(attr: Attr<*>): Boolean = true

    override fun <ST : Any> subquery(subType: KClass<ST>): Query<ST> = this as Query<ST>

}

data class GraphQuery<R : Any>(val type: KClass<R>, val links: Map<String, GraphQuery<*>?>) : Query<R> {

    override fun shouldFetch(attr: Attr<*>): Boolean {
        return attr.name in links || type.propertyFor(attr)?.returnType?.isMarkedNullable == false
    }

    override fun <ST : Any> subquery(subType: KClass<ST>): Query<ST> = GraphQuery(subType, links)

}

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
                        if (list == null || list.isEmpty() || list.firstOrNull()?.let { e -> e::class } in valueTypes) {
                            return@forEach
                        } else {
                            list.forEach { item ->
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
        val entityFacts = IdentityHashMap<Any, List<Fact>>().apply {
            this[e] = e.toFacts()
        }
        return EntityGraphFactorization(entityFacts)
    }

    val res = IdentityHashMap<Any, List<Fact>>()
    val idMap = IdentityHashMap<Any, Gid>()
    val entities = HashMap<Gid, Any>()

    fun body(e: Any): List<Fact> {
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
        val facts: List<Fact> = attrs.flatMap {
            val attr: Attr<*> = schema(e::class.attrName(it))
                    ?: throw QBitException("Attribute for property ${e::class.attrName(it)} isn't defined")
            when (it.returnType.classifier) {
                in valueTypes -> {
                    val value = it.call(e)
                    if (value != null) {
                        listOf(Fact(gid, attr.name, value))
                    } else {
                        emptyList()
                    }
                }
                in collectionTypes -> {
                    ((it.call(e) as List<*>?) ?: emptyList<Any>()).flatMap { v ->
                        when (v!!::class) {
                            in valueTypes -> listOf(Fact(gid, attr.name, v))
                            else -> {
                                body(v)
                                listOf(Fact(gid, attr.name, idMap[v]!!))
                            }
                        }
                    }
                }
                else -> {
                    val value = it.call(e) ?: return@flatMap emptyList<Fact>()

                    val fs = arrayListOf(Fact(gid, attr.name, idMap[value]!!))
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
            .forEach {
                val distinctStates = it.value.map { it.key }.distinct()
                if (distinctStates.size > 1) {
                    throw QBitException("Entity ${it.key} has several different states to store: $distinctStates")
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

val Any.gid: Gid?
    get() {
        if (this is Entity) {
            return this.gid
        }

        val id = this::class.members
                .filterIsInstance<KProperty1<Any, *>>()
                .firstOrNull { it.name == "id" }
                ?.get(this)
        return when (id) {
            null -> null
            is Long -> Gid(id)
            is Gid -> id
            else -> throw QBitException("Unsupported id type: $id of entity $this")
        }
    }

fun KClass<*>.attrName(prop: KProperty1<*, *>): String =
        "." + this.qualifiedName!! + "/" + prop.name

fun KClass<*>.attrName(prop: KProperty0<*>): String =
        "." + this.qualifiedName!! + "/" + prop.name

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
