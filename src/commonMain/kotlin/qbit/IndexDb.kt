package qbit

import qbit.Attrs.list
import qbit.Attrs.name
import qbit.Attrs.type
import qbit.Attrs.unique
import qbit.mapping.reconstruct
import qbit.model.*
import qbit.model.DetachedEntity
import qbit.model.toFacts
import qbit.platform.WeakHashMap
import qbit.platform.set
import kotlin.reflect.KClass

interface QueryPred {
    val attrName: String

    fun compareTo(another: Any): Int
}

fun hasAttr(attr: Attr2<*>): QueryPred =
        AttrPred(attr.name)

fun <T : Any> attrIs(attr: Attr2<T>, value: T): QueryPred =
        AttrValuePred(attr.name, value)

fun <T : Any> attrIn(attr: Attr2<T>, from: T, to: T): QueryPred =
        AttrRangePred(attr.name, from, to)

internal data class AttrPred(override val attrName: String) : QueryPred {

    override fun compareTo(another: Any): Int = 0

}

internal data class AttrValuePred(override val attrName: String, val value: Any) : QueryPred {

    override fun compareTo(another: Any): Int =
            compareValues(another, value)

}

internal data class AttrRangePred(override val attrName: String, val from: Any, val to: Any) : QueryPred {

    override fun compareTo(another: Any): Int {
        val r1 = compareValues(another, from)
        if (r1 < 0) {
            return r1
        }
        val r2 = compareValues(another, to)
        if (r2 > 0) {
            return r2
        }
        return 0
    }

}

interface Db {

    fun pull(eid: Gid): Entity?

    fun <R : Any> pullT(eid: Gid, type: KClass<R>): R?

    // Todo: add check that attrs are presented in schema
    fun query(vararg preds: QueryPred): Sequence<Entity>

    fun attr(attr: String): Attr2<*>?

    fun with(facts: List<Fact>): Db?

}

inline fun <reified R : Any> Db.pullT(eid: Gid): R? {
    return this.pullT(eid, R::class)
}

class IndexDb(internal val index: Index) : Db {
    private val schema = loadAttrs(index)

    private val notFound = DetachedEntity()

    private val entityCache = WeakHashMap<Gid, Entity>()

    private val dcCache = WeakHashMap<Entity, Any>()

    override fun with(facts: List<Fact>): Db? {
        return IndexDb(index.addFacts(facts))
    }

    override fun pull(eid: Gid): Entity? {
        val cached = entityCache[eid]
        if (cached === notFound) {
            return null
        } else if (cached != null) {
            return cached
        }

        val rawEntity = index.entityById(eid)
        if (rawEntity == null) {
            entityCache[eid] = notFound
            return null
        }
        val attrValues = rawEntity.entries.map {
            val attr = schema[it.key]
            require(attr != null) { "There is no attribute with name ${it.key}" }
            require(attr.list || it.value.size == 1) { "Corrupted ${attr.name} of $eid" }
            attr to if (attr.list) it.value else it.value[0]
        }
        val entity = Entity(eid, attrValues, this)
        entityCache[eid] = entity
        return entity
    }

    override fun <R : Any> pullT(eid: Gid, type: KClass<R>): R? {
        val entity = pull(eid) ?: return null
        val cached = dcCache[entity]
        if (cached === notFound) {
            return null
        } else if (cached != null) {
            @Suppress("UNCHECKED_CAST")
            return cached as R
        }

        val dc = reconstruct(type, entity.toFacts(eid), this)
        dcCache[entity] = dc
        return dc
    }

    override fun query(vararg preds: QueryPred): Sequence<Entity> {
        // filters data + base sequence data
        val arrayOfFalse = { Array(preds.size) { false } }

        val base = index.eidsByPred(preds[0])
        val filters: List<Iterator<Gid>> = preds.drop(1).map { index.eidsByPred(it).iterator() }

        // eids, that were fetched from filtering sequences while candidates checking
        val seenEids = hashMapOf<Gid, Array<Boolean>>()

        return base // iterate candidates and check that candidate matches filters
                .filter { cEid ->
                    val filtersSeenEid = seenEids.getOrPut(cEid, arrayOfFalse)

                    if (filtersSeenEid.all { it }) {
                        // the eid already has been returned, so skip duplicate
                        return@filter false
                    }

                    // seek all filters for the eid
                    filtersSeenEid
                            .take(filters.size)
                            .withIndex()
                            .filterNot { f -> f.value } // for filters for which the eid was not seen yet...
                            .forEach { f ->
                                // seek it for the eid
                                val matches = filters[f.index].asSequence() // "enrich" iterator with takeWhile
                                        .onEach { fEid ->
                                            // remember what we saw in process of...
                                            seenEids.getOrPut(fEid, arrayOfFalse)[f.index] = true
                                        }
                                        .any { fEid -> fEid == cEid } // searching of the eid

                                // if the eid has been found, then it has been seen in the filter
                                seenEids.getValue(cEid)[f.index] = matches
                            }

                    // candidate is matches if it has been seen in all filters
                    filtersSeenEid[filtersSeenEid.size - 1] = true // mark, that eid was seen for base sequence
                    filtersSeenEid.all { it }
                }
                .map { pull(it)!! }
    }

    override fun attr(attr: String): Attr2<*>? = schema[attr]

    companion object {

        private fun loadAttrs(index: Index): Map<String, Attr2<*>> {
            val attrEids = index.eidsByPred(hasAttr(name))
            @Suppress("UNCHECKED_CAST")
            val attrFacts = attrEids
                    .map {
                        val e: Map<String, List<Any>> = index.entityById(it)!!
                        val name = e.getValue(name.name)[0] as String
                        val type = e.getValue(type.name)[0] as Byte
                        val unique = e[unique.name]?.firstOrNull() as? Boolean ?: false
                        val list = e[list.name]?.firstOrNull() as? Boolean ?: false
                        val attr = Attr2<Any>(it, name, type, unique, list)
                        (name to attr)
                    }
            return attrFacts.toMap()
        }
    }

}