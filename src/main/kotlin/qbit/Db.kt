package qbit

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

    fun pull(eid: EID): Map<String, Any>? = index.entityById(eid)

    fun entitiesByAttr(attr: String, value: Any) = index.entitiesByAttr(attr, value)

    fun findSubgraph(uuid: DbUuid): Node {
        return graph.findSubgraph(head, uuid)
    }

    /**
     * sgRoot - root of the subgraph
     */
    fun findSubgraph(n: Node, sgRootsHashes: Set<Hash>): Node = when {
        n is Leaf && n.parent.hash in sgRootsHashes -> Leaf(NodeRef(n.parent), n.source, n.timestamp, n.data)
        n is Leaf && n.parent.hash !in sgRootsHashes -> findSubgraph(n.parent, sgRootsHashes)
        n is Merge -> Merge(findSubgraph(n.parent1, sgRootsHashes), findSubgraph(n.parent2, sgRootsHashes), n.source, n.timestamp, n.data)
        else -> throw AssertionError("Should never happen, n is $n root is $sgRootsHashes")
    }

}