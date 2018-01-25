package qbit

import java.util.*

class Db(val head: NodeVal, resolve: (NodeRef) -> NodeVal?) {

    private val graph = Graph(resolve)
    private val index = createIndex(graph, head)

    companion object {
        private fun createIndex(graph: Graph, head: Node): Index {
            var res = Index()
            graph.walk(head, { n ->
                if (n is NodeVal) {
                    res = n.data.trx.fold(res, { acc, f -> acc.add(StoredFact(f.entityId, f.attribute, n.timestamp, f.value)) })
                }
                false
            })
            return res
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

    /**
     * sgRoot - root of the subgraph
     */
    fun findSubgraph(n: Node, sgRootsHashes: Set<String>): Node = when {
        n is Leaf && n.parent.hash.toHexString() in sgRootsHashes -> Leaf(NodeRef(n.parent), n.source, n.timestamp, n.data)
        n is Leaf && n.parent.hash.toHexString() !in sgRootsHashes -> findSubgraph(n.parent, sgRootsHashes)
        n is Merge -> Merge(findSubgraph(n.parent1, sgRootsHashes), findSubgraph(n.parent2, sgRootsHashes), n.source, n.timestamp, n.data)
        else -> throw AssertionError("Should never happen, n is $n root is $sgRootsHashes")
    }

}