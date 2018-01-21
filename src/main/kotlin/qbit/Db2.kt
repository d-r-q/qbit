package qbit

class Db2(val head: Node, val resolve: (String) -> NodeVal?) {

    val graph = Graph(resolve)
    private val index = createIndex(graph, head)

    companion object {
        private fun createIndex(graph: Graph, head: Node): Index {
            val res = Index()
            graph.walk(head, { n ->
                if (n is Leaf) {
                    n.data.trx.forEach { res.add(StoredFact(it.entityId, it.attribute, n.timestamp, it.value)) }
                }
                false
            })
            return res;
        }
    }

    fun pull(eid: EID): Map<String, Any>? {
        try {
            val res = HashMap<String, Any>()
            graph.walk(head) {
                if (it is NodeVal) {
                    it.data.trx.filter { it.entityId == eid }.forEach {
                        res.putIfAbsent(it.attribute, it.value)
                    }
                }
                false
            }
            return res.takeIf { it.size > 0 }
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    fun findSubgraph(uuid: DbUuid): Node {
        return graph.findSubgraph(head, uuid)
    }

}