package qbit

import qbit.schema.Attr
import qbit.schema.Schema

class Db(val head: NodeVal<Hash>, resolve: (NodeRef) -> NodeVal<Hash>?) {

    private val graph = Graph(resolve)
    private val index = createIndex(graph, head)
    private val schema = Schema(loadAttrs(index))

    fun pull(eid: EID): StoredEntity? = index.entityById(eid)?.let {
        Entity(eid, it.map { schema.find(it.key)!! to it.value }) }

    fun <T : Any> entitiesByAttr(attr: Attr<T>, value: T? = null): List<StoredEntity> {
        val eids =
                if (value != null) index.entitiesByAttrVal(attr.str, value)
                else index.entitiesByAttr(attr.str)

        return eids.map { pull(it)!! }
    }

    fun findSubgraph(uuid: DbUuid): Node<Hash> {
        return graph.findSubgraph(head, uuid)
    }

    fun attr(attr: String): Attr<*>? = schema.find(attr)

    companion object {

        fun createIndex(graph: Graph, head: NodeVal<Hash>): Index {
            val parentIdx =
                    when (head) {
                        is Root -> Index()
                        is Leaf -> createIndex(graph, graph.resolveNode(head.parent))
                        is Merge -> {
                            val idx1 = createIndex(graph, graph.resolveNode(head.parent1))
                            val idx2 = createIndex(graph, graph.resolveNode(head.parent2))
                            idx2.add(idx1.eavt.filterIsInstance<StoredFact>().toList())
                        }
                    }
            return parentIdx.add(head.data.trx.map { StoredFact(it.eid, it.attr, head.timestamp, it.value) })
        }

        private fun loadAttrs(index: Index): Map<String, Attr<*>> {
            val factsByAttr: List<StoredFact> = index.factsByAttr(qbit.schema._name.str)
            return factsByAttr
                    .map {
                        val e = index.factsByEid(it.eid)!!
                        val name = e[qbit.schema._name.str]!! as String
                        val type = e[qbit.schema._type.str]!! as Byte
                        val unique = e[qbit.schema._unique.str] as? Boolean ?: false
                        name to Attr(name, DataType.ofCode(type)!!, unique)
                    }
                    .toMap()
        }
    }

}
