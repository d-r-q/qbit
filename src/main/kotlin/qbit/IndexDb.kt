package qbit

import qbit.EAttr.list
import qbit.EAttr.name
import qbit.EAttr.type
import qbit.EAttr.unique
import qbit.schema.*

interface QueryPred {
    val attrName: String

    fun compareTo(another: Any): Int
}

fun hasAttr(attr: Attr<*>): QueryPred =
        AttrPred(attr.str())

fun <T : Any> attrIs(attr: Attr<T>, value: T): QueryPred =
        AttrValuePred(attr.str(), value)

fun <T : Any> attrIn(attr: Attr<T>, from: T, to: T): QueryPred =
        AttrRangePred(attr.str(), from, to)

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

    fun pull(eid: EID): StoredEntity?

    fun query(vararg preds: QueryPred): Sequence<StoredEntity>

    fun attr(attr: String): Attr<Any>?

}

class IndexDb(internal val index: Index) : Db {

    private val schema = loadAttrs(index)

    override fun pull(eid: EID): StoredEntity? {
        val entity = index.entityById(eid) ?: return null
        val attrValues = entity.entries.map {
            val attr = schema[it.key]
            require(attr != null)
            attr to it.value
        }
        return Entity(eid, attrValues, this)
    }

    override fun query(vararg preds: QueryPred): Sequence<StoredEntity> {
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
                            .forEach { f -> // seek it for the eid
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

    override fun attr(attr: String): Attr<Any>? = schema[attr]

    companion object {

        private fun loadAttrs(index: Index): Map<String, Attr<Any>> {
            val attrEidss = index.eidsByPred(hasAttr(name))
            @Suppress("UNCHECKED_CAST")
            val attrFacts = attrEidss
                    .map {
                        val e = index.entityById(it)!!
                        val name = e.getValue(name.str()) as String
                        val type = e.getValue(type.str()) as Byte
                        val unique = e[unique.str()] as? Boolean ?: false
                        val list = e[list.str()] as? Boolean ?: false
                        val attr: Attr<Any> =
                                when {
                                    list && type == QEntity.code -> RefListAttr(name, unique)
                                    type == QEntity.code -> RefAttr(name, unique)
                                    list -> ListAttr(name, DataType.ofCode(type)!!, unique)
                                    else -> Attr(name, DataType.ofCode(type)!!, unique)
                                }
                        (name to attr)
                    }
            return attrFacts.toMap()
        }
    }

}