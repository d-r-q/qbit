package qbit.mapping

import qbit.*
import qbit.model.*
import qbit.platform.IdentityHashMap
import qbit.platform.set
import kotlin.reflect.*


val collectionTypes = setOf(List::class)

val types: Map<KClass<*>, DataType<*>> = mapOf(
        String::class to QString,
        Byte::class to QByte,
        Int::class to QInt,
        Long::class to QLong,
        Boolean::class to QBoolean,
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
                        if (list == null || list.isEmpty() || list[0]::class in valueTypes) {
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

fun destruct(e: Any, schema: (String) -> Attr<*>?, gids: Iterator<Gid>): List<Fact> {
    if (e is Tombstone) {
        return e.toFacts()
    }
    validate(e::class, findProperties(e::class))
    val res = IdentityHashMap<Any, List<Fact>>()
    val idMap = IdentityHashMap<Any, Gid>()

    fun body(e: Any): List<Fact> {
        if (res.containsKey(e)) {
            return res[e]!!
        } else {
            res[e] = emptyList()
        }

        val getters = e::class.members
                .filterIsInstance<KProperty1<*, *>>()
                .sortedBy { it.name }

        val (id, attrs) = getters.partition { it.name == "id" && it.returnType.classifier == Long::class || it.returnType.classifier == Gid::class }
        val eid = idMap[e]!! // ids existence has been checked while identification
        val facts: List<Fact> = attrs.flatMap {
            val attr: Attr<*> = schema(e::class.attrName(it))
                    ?: throw QBitException("Attribute for property ${e::class.attrName(it)} isn't defined")
            when (it.returnType.classifier) {
                in valueTypes -> listOf(Fact(eid, attr.name, it.call(e)!!))
                in collectionTypes -> {
                    (it.call(e) as List<*>).flatMap { v ->
                        when (v!!::class) {
                            in valueTypes -> listOf(Fact(eid, attr.name, v))
                            else -> {
                                body(v)
                                listOf(Fact(eid, attr.name, idMap[v]!!))
                            }
                        }
                    }
                }
                else -> {
                    val value = it.call(e) ?: return@flatMap emptyList<Fact>()

                    val fs = arrayListOf(Fact(eid, attr.name, idMap[value]!!))
                    body(value)
                    fs
                }
            }
        }
        res[e] = facts
        return facts
    }

    identifyEntityGraph(e, gids, idMap)
    body(e)

    return res.values.flatten()
}

fun validate(type: KClass<*>, getters: List<KCallable<*>>) {
    val listsOfNullables = getters.filter { it.returnType.classifier == List::class && it.returnType.arguments[0].type!!.isMarkedNullable }
    if (listsOfNullables.isNotEmpty()) {
        val props = "${type.simpleName}.${listsOfNullables.map { it.name }.joinToString(",", "(", ")")}"
        throw QBitException("List of nullable elements is not supported. Properties: $props")
    }
}

val Any.id: Long
    get() {
        val id = this::class.members
                .filterIsInstance<KProperty1<Any, *>>()
                .firstOrNull { it.name == "id" }
                ?.get(this)
        return when (id) {
            is Long -> id
            is Gid -> id.value()
            else -> throw QBitException("Unsupported id type: $id of entity $this")
        }
    }

val Any.gid: Gid
    get() {
        val id = this::class.members
                .filterIsInstance<KProperty1<Any, *>>()
                .firstOrNull { it.name == "id" }
                ?.get(this)
        return when (id) {
            is Long -> Gid(id)
            is Gid -> id
            else -> throw QBitException("Unsupported id type: $id")
        }
    }

fun castGid(gid: Any, type: KClassifier) =
        when {
            gid is Gid && type == Long::class -> gid.value()
            gid is Gid && type == Gid::class -> gid
            gid is Long && type == Long::class -> gid
            gid is Long && type == Gid::class -> Gid(gid)
            else -> throw QBitException("Cannot cast $gid to $type")
        }

fun <R : Any> reconstruct(type: KClass<R>, facts: Collection<Fact>, db: Db, fetch: Fetch = qbit.Lazy): R {
    return reconstruct(GraphQuery(type, mapOf()), facts, db, fetch)
}

fun <R : Any> reconstruct(query: GraphQuery<R>, facts: Collection<Fact>, db: Db, fetch: Fetch = qbit.Lazy): R {
    require(facts.distinctBy { it.eid }.size == 1) { "Too many expected exactly one entity: ${facts.distinctBy { it.eid }}" }
    val attrFacts = facts.groupBy { it.attr }
    val constr = query.type.constructors.first()
    val attrParams =
            attrFacts.map { f ->
                val param = constr.parameters.find { it.name == f.value[0].attr.substringAfter("/") } ?: return@map null
                when (param.type.classifier) {
                    in valueTypes -> param to f.value[0].value
                    List::class -> {
                        when (param.type.arguments[0].type!!.classifier as KClass<*>) {
                            in valueTypes -> param to f.value.map { it.value }
                            else -> param to f.value.map { db.pull(f.value[0].value as Gid, param.type.arguments[0].type!!.classifier as KClass<*>, fetch) }
                        }
                    }
                    else -> {
                        if (param.type.isMarkedNullable) {
                            if (query.links.contains(param.name) || fetch == Eager) {
                                param to db.pull(f.value[0].value as Gid, param.type.classifier as KClass<*>, fetch)
                            } else {
                                param to null
                            }
                        } else {
                            param to db.pull(f.value[0].value as Gid, param.type.classifier as KClass<*>, fetch)
                        }
                    }
                }
            }
                    .filterNotNull()
    val idProp = constr.parameters.find { it.name == "id" }!!
    val id = if (idProp.type.classifier == Gid::class) {
        facts.first().eid
    } else {
        facts.first().eid.value()
    }
    val idParam = (idProp to id)
    return constr.callBy((attrParams + idParam).toMap())
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
                    in valueTypes -> Attr(null, type.attrName(it), types.getValue(valueType).code, type.attrName(it) in unique, true)
                    else -> Attr<Any>(null, type.attrName(it), QRef.code, type.attrName(it) in unique, true)
                }
            }
            else -> {
                Attr(null, type.attrName(it), QRef.code, type.attrName(it) in unique, false)
            }
        }
    }
}
