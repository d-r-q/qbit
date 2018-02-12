package qbit

class Db(val head: NodeVal<Hash>, resolve: (NodeRef) -> NodeVal<Hash>?) {

    private val graph = Graph(resolve)
    private val index = createIndex(graph, head)

    companion object {
        fun createIndex(graph: Graph, head: NodeVal<Hash>): Index {
            val parentIdx =
                    when (head) {
                        is Root -> Index()
                        is Leaf -> createIndex(graph, graph.resolve(head.parent))
                        is Merge -> {
                            val idx1 = createIndex(graph, graph.resolve(head.parent1))
                            val idx2 = createIndex(graph, graph.resolve(head.parent2))
                            idx2.add(idx1.eavt.filterIsInstance<StoredFact>().toList())
                        }
                    }
            return parentIdx.add(head.data.trx.map { StoredFact(it.entityId, it.attribute, head.timestamp, it.value) })
        }

        private fun Graph.resolve(n: Node<Hash>) = when (n) {
            is NodeVal<Hash> -> n
            is NodeRef -> this.resolve(n) ?: throw QBitException("Corrupted graph, could not resovle $n")
        }

    }

    fun pull(eid: EID): StoredEntity? = index.entityById(eid)?.let { MapEntity(eid, it) }

    fun entitiesByAttr(attr: String, value: Any? = null): List<StoredEntity> {
        val eids =
                if (value != null) index.entitiesByAttrVal(attr, value)
                else index.entitiesByAttr(attr)

        return eids.map { pull(it)!! }
    }

    fun findSubgraph(uuid: DbUuid): Node<Hash> {
        return graph.findSubgraph(head, uuid)
    }

    /**
     * sgRoot - root of the subgraph
     */
    fun findSubgraph(n: Node<Hash>, sgRootsHashes: Set<Hash>): Node<Hash> = when {
        n is Leaf && n.parent.hash in sgRootsHashes -> Leaf(n.hash, NodeRef(n.parent), n.source, n.timestamp, n.data)
        n is Leaf && n.parent.hash !in sgRootsHashes -> findSubgraph(n.parent, sgRootsHashes)
        n is Merge -> Merge(n.hash, findSubgraph(n.parent1, sgRootsHashes), findSubgraph(n.parent2, sgRootsHashes), n.source, n.timestamp, n.data)
        else -> throw AssertionError("Should never happen, n is $n root is $sgRootsHashes")
    }

}

private class MapEntity(
        override val eid: EID,
        private val map: Map<String, Any>
) :
        StoredEntity,
        Map<String, Any> by map