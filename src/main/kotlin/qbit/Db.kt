package qbit

import qbit.schema.Attr
import qbit.schema.Schema

interface AttrP<T : Any> {
    val attr: Attr<T>

    fun compareTo(another: T): Int
}

data class AttrValue<T : Any>(override val attr: Attr<T>, val value: T) : AttrP<T> {
    override fun compareTo(another: T): Int =
            compareValues(value, another)
}

data class AttrRange<T : Any>(override val attr: Attr<T>, val from: T, val to: T) : AttrP<T> {
    override fun compareTo(another: T): Int {
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

class Db(private val index: Index) {

    private val schema = Schema(loadAttrs(index))

    fun pull(eid: EID): StoredEntity? {
        val entity = index.entityById(eid) ?: return null
        val attrValues = entity.map {
            val attr = schema.find(it.key)
            require(attr != null)
            attr!! to it.value
        }
        return Entity(eid, attrValues)
    }

    fun <T : Any> entitiesByAttr(attr: Attr<T>, value: T? = null): List<StoredEntity> {
        val eids =
                if (value != null) index.entitiesByAttrVal(attr.str, value)
                else index.entitiesByAttr(attr.str)

        return eids.map { pull(it)!! }
    }

    fun query(vararg preds: AttrP<*>): List<StoredEntity> {
        val eidSets = ArrayList<Set<EID>>(preds.size)
        var minSet: Set<EID>? = null
        for (pred in preds) {
            eidSets.add(index.entitiesByPred(pred as AttrP<Any>))
            if (minSet == null || eidSets.last().size < minSet.size) {
                minSet = eidSets.last()
            }
        }
        eidSets.remove(minSet)
        val res = minSet!!.toMutableSet()
        res.removeIf { e -> eidSets.any { !it.contains(e) } }
        return res.map { pull(it)!! }
    }

    fun attr(attr: String): Attr<*>? = schema.find(attr)

    companion object {

        private fun loadAttrs(index: Index): Map<String, Attr<*>> {
            val attrs = index.entitiesByAttr(qbit.schema._name.str)
            return attrs
                    .map {
                        val e = index.entityById(it)!!
                        val name = e[qbit.schema._name.str]!! as String
                        val type = e[qbit.schema._type.str]!! as Byte
                        val unique = e[qbit.schema._unique.str] as? Boolean ?: false
                        name to Attr(name, DataType.ofCode(type)!!, unique)
                    }
                    .toMap()
        }
    }

}
