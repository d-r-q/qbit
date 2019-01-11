package qbit

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

    fun query(vararg preds: QueryPred): List<StoredEntity>

    fun attr(attr: String): Attr<Any>?

}

class IndexDb(internal val index: Index) : Db {

    private val schema = Schema(loadAttrs(index))

    override fun pull(eid: EID): StoredEntity? {
        val entity = index.entityById(eid) ?: return null
        val attrValues = entity.entries.map {
            val attr = schema.find(it.key)
            require(attr != null)
            attr to it.value
        }
        return Entity(eid, attrValues, this)
    }

    override fun query(vararg preds: QueryPred): List<StoredEntity> {
        val eidSets = ArrayList<Set<EID>>(preds.size)
        var minSet: Set<EID>? = null
        for (pred in preds) {
            eidSets.add(index.eidsByPred(pred))
            if (minSet == null || eidSets.last().size < minSet.size) {
                minSet = eidSets.last()
            }
        }
        eidSets.remove(minSet)
        val res = minSet!!.toMutableSet()
        if (eidSets.size > 0) {
            res.removeIf { e -> eidSets.any { !it.contains(e) } }
        }
        return res.map { pull(it)!! }
    }

    override fun attr(attr: String): Attr<Any>? = schema.find(attr)

    companion object {

        private fun loadAttrs(index: Index): Map<String, Attr<Any>> {
            val attrEidss = index.eidsByPred(hasAttr(qbit.schema._name))
            @Suppress("UNCHECKED_CAST")
            val attrFacts = attrEidss
                    .map {
                        val e = index.entityById(it)!!
                        val name = e[_name.str()]!! as String
                        val type = e[_type.str()]!! as Byte
                        val unique = e[_unique.str()] as? Boolean ?: false
                        val attr: Attr<Any> =
                                if (type == QEntity.code) {
                                    RefAttr(name, unique) as Attr<Any>
                                } else {
                                    Attr(name, DataType.ofCode(type)!!, unique) as Attr<Any>
                                }
                        (name to attr)
                    }
            return attrFacts.toMap()
        }
    }

}