package qbit.mapping

import qbit.Db
import qbit.Fact
import qbit.QBitException
import qbit.model.*
import qbit.platform.IdentityHashMap
import qbit.platform.set
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1


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

data class Query<R : Any>(val type: KClass<R>, val links: Map<String, Query<*>?>)

fun identifyEntityGraph(root: Any, eids: Iterator<Gid>, idMap: IdentityHashMap<Any, Any>): Any {
    val idProp = root::class.members
            .firstOrNull {
                it is KProperty1<*, *> && it.name == "id" && it.returnType.classifier == Long::class || it.returnType.classifier == Gid::class
            }
            ?: throw IllegalArgumentException("Entity $root does not contains `id: Long` property")

    var id = idProp.call(root) ?: idMap[root]
    var needUpdate = id == null
    id = id ?: eids.next().let {
        if (idProp.returnType.classifier == Gid::class) {
            it
        } else {
            it.value()
        }
    }

    val attrs = root::class.constructors.first().parameters
            .map {
                val prop = root::class.members.firstOrNull { m -> m.name == it.name }!!
                if (prop == idProp) {
                    return@map it to id
                }
                val value = prop.call(root)
                when (it.type.classifier) {
                    null -> it to value
                    in valueTypes -> it to value
                    in collectionTypes -> {
                        @Suppress("UNCHECKED_CAST")
                        val list = value as List<Any>?
                        if (list == null || list.isEmpty() || list[0]::class in valueTypes) {
                            return@map it to value
                        }
                        val newList = list.map { item ->
                            val newItem = identifyEntityGraph(item, eids, idMap)
                            needUpdate = needUpdate || item !== newItem
                            newItem
                        }
                        it to newList
                    }
                    else -> {
                        val newValue = identifyEntityGraph(value!!, eids, idMap)
                        needUpdate = needUpdate || value !== newValue
                        it to newValue
                    }
                }
            }
            .toMap()

    return if (needUpdate) {
        val identified = root::class.constructors.first().callBy(attrs)
        idMap[root] = identified
        identified
    } else {
        if (!idMap.containsKey(root)) {
            idMap[root] = root
        }
        idMap[root]!!
    }
}

fun destruct(e: Any, schema: (String) -> Attr2<*>?, eids: Iterator<Gid>): List<Fact> {
    val res = IdentityHashMap<Any, List<Fact>>()
    val idMap = IdentityHashMap<Any, Any>()

    fun body(e: Any): List<Fact> {
        if (res.containsKey(e)) {
            return res[e]!!
        }
        val getters = e::class.members.filterIsInstance<KProperty1<*, *>>()
        val (_, attrs) = getters.partition { it.name == "id" && it.returnType.classifier == Long::class || it.returnType.classifier == Gid::class }
        val eid = Gid(e.id) // ids existence has been checked while identification
        val facts: List<Fact> = attrs.flatMap {
            val attr: Attr2<*> = schema(e::class.attrName(it))
                    ?: throw QBitException("Attribute for property ${e::class.attrName(it)} isn't defined")
            when (it.returnType.classifier) {
                in valueTypes -> listOf(Fact(eid, attr.name, it.call(e)!!))
                in collectionTypes -> {
                    (it.call(e) as List<*>).flatMap { v ->
                        when (v!!::class) {
                            in valueTypes -> listOf(Fact(eid, attr.name, v))
                            else -> {
                                body(v)
                                listOf(Fact(eid, attr.name, Gid(v.id)))
                            }
                        }
                    }
                }
                else -> {

                    val fs = arrayListOf(Fact(eid, attr.name, Gid(it.call(e)!!.id)))
                    body(it.call(e)!!)
                    fs
                }
            }
        }
        res[e] = facts
        return facts
    }

    body(identifyEntityGraph(e, eids, idMap))

    return res.values.flatten()
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
            else -> throw QBitException("Unsupported id type: $id")
        }
    }

val Any.eid: Gid
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

fun <R : Any> reconstruct(type: KClass<R>, facts: Collection<Fact>, db: Db): R {
    return reconstruct(Query(type, mapOf()), facts, db)
}

fun <R : Any> reconstruct(query: Query<R>, facts: Collection<Fact>, db: Db): R {
    require(facts.distinctBy { it.eid }.size == 1) { "Too many expected exactly one entity: ${facts.distinctBy { it.eid }}" }
    val attrFacts = facts.groupBy { it.attr }
    val constr = query.type.constructors.first()
    val attrParams =
            attrFacts.map { f ->
                val param = constr.parameters.find { it.name == f.value[0].attr.substringAfter("/") }!!
                when (param.type.classifier) {
                    in valueTypes -> param to f.value[0].value
                    List::class -> {
                        when (param.type.arguments[0].type!!.classifier as KClass<*>) {
                            in valueTypes -> param to f.value.map { it.value }
                            else -> param to f.value.map { db.pullT(f.value[0].value as Gid, param.type.arguments[0].type!!.classifier as KClass<*>) }
                        }
                    }
                    else -> {
                        if (param.type.isMarkedNullable) {
                            if (query.links.contains(param.name)) {
                                param to db.pullT(f.value[0].value as Gid, param.type.classifier as KClass<*>)
                            } else {
                                param to null
                            }
                        } else {
                            param to db.pullT(f.value[0].value as Gid, param.type.classifier as KClass<*>)
                        }
                    }
                }
            }
    val idParam = (constr.parameters.find { it.name == "id" }!! to facts.first().eid.value())
    return constr.callBy((attrParams + idParam).toMap())
}

fun KClass<*>.attrName(prop: KProperty1<*, *>): String =
        "." + this.qualifiedName!! + "/" + prop.name

fun KClass<*>.attrName(prop: KProperty0<*>): String =
        "." + this.qualifiedName!! + "/" + prop.name

fun schemaFor(type: KClass<*>, unique: Set<String>): Collection<Attr2<*>> {
    val getters = type.members.filterIsInstance<KProperty1<*, *>>()
    val (ids, attrs) = getters.partition { it.name == "id" && it.returnType.classifier == Long::class }
    val id = ids.firstOrNull()
    id ?: throw IllegalArgumentException("Type $type does not contains id: Long property")
    return attrs.map {
        when (it.returnType.classifier) {
            in valueTypes -> Attr2(null, type.attrName(it), types.getValue(it.returnType.classifier as KClass<*>).code, type.attrName(it) in unique, false)
            in collectionTypes -> {
                when (val valueType = it.returnType.arguments[0].type!!.classifier as KClass<*>) {
                    in valueTypes -> Attr2(null, type.attrName(it), types.getValue(valueType).code, type.attrName(it) in unique, true)
                    else -> Attr2<Any>(null, type.attrName(it), QRef.code, type.attrName(it) in unique, true)
                }
            }
            else -> {
                Attr2(null, type.attrName(it), QRef.code, type.attrName(it) in unique, false)
            }
        }
    }
}
