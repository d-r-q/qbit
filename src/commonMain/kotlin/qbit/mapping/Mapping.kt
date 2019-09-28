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

val scalarTypes = setOf(String::class, Long::class, Byte::class, Boolean::class)

val collectionTypes = setOf(List::class)

val types = mapOf(String::class to QString, Long::class to QLong, Byte::class to QByte, Boolean::class to QBoolean)

data class Query<R : Any>(val type: KClass<R>, val links: Map<String, Query<*>?>)


fun destruct(e: Any, schema: (String) -> Attr2?, eids: Iterator<EID>): List<Fact> {
    val res = IdentityHashMap<Any, List<Fact>>()

    fun body(e: Any): List<Fact> {
        if (res.containsKey(e)) {
            return res[e]!!
        }
        val getters = e::class.members.filterIsInstance<KProperty1<*, *>>()
        val (ids, attrs) = getters.partition { it.name == "id" && it.returnType.classifier == Long::class || it.returnType.classifier == EID::class }
        val id = ids.firstOrNull()
        id ?: throw IllegalArgumentException("Entity $e does not contains id: Long property")
        val eid = id.call(e).let {
            when (it) {
                null -> eids.next()
                is Long -> EID(it)
                is EID -> it
                else -> throw AssertionError("Unexpected id: $it")
            }
        }
        val facts: List<Fact> = attrs.flatMap {
            val attr: Attr2 = schema(e::class.attrName(it))
                    ?: throw QBitException("Attribute for property ${e::class.attrName(it)} isn't defined")
            when (it.returnType.classifier) {
                in scalarTypes -> listOf(Fact(eid, attr.name, it.call(e)!!))
                in collectionTypes -> {
                    (it.call(e) as List<*>).flatMap { v ->
                        when (v!!::class) {
                            in scalarTypes -> listOf(Fact(eid, attr.name, v))
                            else -> {
                                val fs = body(v)
                                fs + Fact(eid, attr.name, v.id ?: fs[0].eid)
                            }
                        }
                    }
                }
                else -> {
                    val fs = body(it.call(e)!!)
                    fs + Fact(eid, attr.name, it.call(e)!!.id ?: fs[0].eid)
                }
            }
        }
        res[e] = facts
        return facts
    }

    body(e)

    return res.values.flatten()
}

val Any.id: Long?
    get() = this::class.members
            .filterIsInstance<KProperty1<Any, Long>>()
            .first { it.name == "id" }
            .get(this)

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
                    in scalarTypes -> param to f.value[0].value
                    List::class -> {
                        when (param.type.arguments[0].type!!.classifier as KClass<*>) {
                            in scalarTypes -> param to f.value.map { it.value }
                            else -> param to f.value.map { db.pullT(f.value[0].value as EID, param.type.arguments[0].type!!.classifier as KClass<*>) }
                        }
                    }
                    else -> {
                        if (param.type.isMarkedNullable) {
                            if (query.links.contains(param.name)) {
                                param to db.pullT(f.value[0].value as EID, param.type.classifier as KClass<*>)
                            } else {
                                param to null
                            }
                        } else {
                            param to db.pullT(f.value[0].value as EID, param.type.classifier as KClass<*>)
                        }
                    }
                }
            }
    val idParam = (constr.parameters.find { it.name == "id" }!! to facts.first().eid.value())
    return constr.callBy((attrParams + idParam).toMap())
}

fun KClass<*>.attrName(prop: KProperty1<*, *>) =
        "." + this.qualifiedName!! + "/" + prop.name

fun KClass<*>.attrName(prop: KProperty0<*>) =
        "." + this.qualifiedName!! + "/" + prop.name

fun schemaFor(type: KClass<*>, unique: Set<String>): Collection<Attr2> {
    val getters = type.members.filterIsInstance<KProperty1<*, *>>()
    val (ids, attrs) = getters.partition { it.name == "id" && it.returnType.classifier == Long::class }
    val id = ids.firstOrNull()
    id ?: throw IllegalArgumentException("Type $type does not contains id: Long property")
    return attrs.map {
        when (it.returnType.classifier) {
            in scalarTypes -> Attr2(null, type.attrName(it), types[it.returnType.classifier as KClass<*>]!!.code, type.attrName(it) in unique, false)
            in collectionTypes -> {
                when (val valueType = it.returnType.arguments[0].type!!.classifier as KClass<*>) {
                    in scalarTypes -> Attr2(null, type.attrName(it), types[valueType]!!.code, type.attrName(it) in unique, true)
                    else -> Attr2(null, type.attrName(it), QRef.code, type.attrName(it) in unique, true)
                }
            }
            else -> {
                Attr2(null, type.attrName(it), QRef.code, type.attrName(it) in unique, false)
            }
        }
    }
}