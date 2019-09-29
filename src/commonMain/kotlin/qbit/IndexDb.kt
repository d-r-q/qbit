package qbit

import qbit.Attrs.list
import qbit.Attrs.name
import qbit.Attrs.type
import qbit.Attrs.unique
import qbit.mapping.reconstruct
import qbit.model.Attr2
import qbit.model.EID
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

    fun pull(eid: EID): Map<String, List<Any>>?

    fun <R : Any> pullT(eid: EID, type: KClass<R>): R?

    // Todo: add check that attrs are presented in schema
    fun query(vararg preds: QueryPred): Sequence<Map<String, List<Any>>>

    fun attr(attr: String): Attr2<*>?

    fun with(facts: List<Fact>): Db?

}

inline fun <reified R : Any> Db.pullT(eid: EID): R? {
    return this.pullT(eid, R::class)
}

class IndexDb(internal val index: Index) : Db {
    private val schema = loadAttrs(index)

    private val NotFound = mapOf<String, List<Any>>()

    private val entityCache = WeakHashMap<EID, Map<String, List<Any>>>()

    private val dcCache = WeakHashMap<Map<String, List<Any>>, Any>()

    override fun with(facts: List<Fact>): Db? {
        return IndexDb(index.addFacts(facts))
    }

    override fun pull(eid: EID): Map<String, List<Any>>? {
        val cached = entityCache[eid]
        if (cached === NotFound) {
            return null
        } else if (cached != null) {
            return cached
        }

        val rawEntity = index.entityById(eid)
        if (rawEntity == null) {
            entityCache[eid] = NotFound
            return null
        }
        entityCache[eid] = rawEntity
        return rawEntity
    }

    override fun <R : Any> pullT(eid: EID, type: KClass<R>): R? {
        val entity = pull(eid) ?: return null
        val cached = dcCache[entity]
        if (cached === NotFound) {
            return null
        } else if (cached != null) {
            @Suppress("UNCHECKED_CAST")
            return cached as R
        }

        val dc = reconstruct(type, entity.toFacts(eid), this)
        dcCache[entity] = dc
        return dc
    }

    private fun Map<String, List<Any>>.toFacts(eid: EID) = this.entries.flatMap { a ->
        a.value.map { i ->
            Fact(eid, a.key, i)
        }
    }

    override fun query(vararg preds: QueryPred): Sequence<Map<String, List<Any>>> {
        // filters data + base sequence data
        val arrayOfFalse = { Array(preds.size) { false } }

        val base = index.eidsByPred(preds[0])
        val filters: List<Iterator<EID>> = preds.drop(1).map { index.eidsByPred(it).iterator() }

        // eids, that were fetched from filtering sequences while candidates checking
        val seenEids = hashMapOf<EID, Array<Boolean>>()

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
            val attrEidss = index.eidsByPred(hasAttr(name))
            @Suppress("UNCHECKED_CAST")
            val attrFacts = attrEidss
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