package qbit.mapping

import qbit.Db
import qbit.Fact
import qbit.model.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

val scalarTypes = setOf(String::class, Long::class)

val collectionTypes = setOf(List::class)

val types = mapOf(String::class to QString, Long::class to QLong)

data class Query<R : Any>(val type: KClass<R>, val links: Map<String, Query<*>?>)

fun destruct(e: Any, db: Db, eids: Iterator<EID>): List<Fact> {
    val getters = e::class.members.filterIsInstance<KProperty1<*, *>>()
    val (ids, attrs) = getters.partition { it.name == "id" && it.returnType.classifier == Long::class }
    val id = ids.firstOrNull()
    id ?: throw IllegalArgumentException("Entity $e does not contains id: Long property")
    val eid = id.call(e)?.let { EID(it as Long) } ?: eids.next()
    return attrs.flatMap {
        val attr: Attr<*> = db.attr(e::class.attrName(it))!!
        when (it.returnType.classifier) {
            in scalarTypes -> listOf(Fact(eid, attr, it.call(e)!!))
            in collectionTypes -> {
                (it.call(e) as List<*>).flatMap { v ->
                    when (v!!::class) {
                        in scalarTypes -> listOf(Fact(eid, attr, v))
                        else -> {
                            val fs = destruct(v, db, eids)
                            fs + Fact(eid, attr, v.id ?: fs[0].eid)
                        }
                    }
                }
            }
            else -> {
                val fs = destruct(it.call(e)!!, db, eids)
                fs + Fact(eid, attr, it.call(e)!!.id ?: fs[0].eid)
            }
        }
    }
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
        "." + this.qualifiedName!! + "/" + prop.attrName

val KProperty1<*, *>.attrName
    get() = this.name

fun schemaFor(type: KClass<*>): Collection<Attr<*>> {
    val getters = type.members.filterIsInstance<KProperty1<*, *>>()
    val (ids, attrs) = getters.partition { it.name == "id" && it.returnType.classifier == Long::class }
    val id = ids.firstOrNull()
    id ?: throw IllegalArgumentException("Type $type does not contains id: Long property")
    return attrs.map {
        when (it.returnType.classifier) {
            in scalarTypes -> Attr(type.attrName(it), types[it.returnType.classifier as KClass<*>]!!)
            in collectionTypes -> {
                when (val valueType = it.returnType.arguments[0].type!!.classifier as KClass<*>) {
                    in scalarTypes -> ListAttr(type.attrName(it), types[valueType]!!)
                    else -> RefListAttr(type.attrName(it))
                }
            }
            else -> {
                RefAttr(type.attrName(it))
            }
        }
    }
}