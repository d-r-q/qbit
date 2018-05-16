package qbit

import qbit.schema.Attr
import qbit.schema.Schema

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

    fun attr(attr: String): Attr<*>? = schema.find(attr)

    companion object {

        private fun loadAttrs(index: Index): Map<String, Attr<*>> {
            val factsByAttr = index.factsByAttr(qbit.schema._name.str)
            return factsByAttr
                    .map {
                        val e = index.entityById(it.eid)!!
                        val name = e[qbit.schema._name.str]!! as String
                        val type = e[qbit.schema._type.str]!! as Byte
                        val unique = e[qbit.schema._unique.str] as? Boolean ?: false
                        name to Attr(name, DataType.ofCode(type)!!, unique)
                    }
                    .toMap()
        }
    }

}
