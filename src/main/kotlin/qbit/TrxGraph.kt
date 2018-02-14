package qbit

import qbit.schema.Attr

data class DbUuid(val iid: IID)

fun Fact(eid: EID, attr: Attr<*>, value: Any) = Fact(eid, attr.str, value)

data class Fact(val eid: EID, val attr: String, val value: Any)

class NodeData(val trx: Array<out Fact>)

sealed class Node<out H : Hash?>(val hash: H)

class NodeRef(hash: Hash) : Node<Hash>(hash) {
    constructor(n: Node<Hash>) : this(n.hash)
}

sealed class NodeVal<out H : Hash?>(hash: H, val source: DbUuid, val timestamp: Long, val data: NodeData) : Node<H>(hash)

class Root<out H : Hash?>(hash: H, source: DbUuid, timestamp: Long, data: NodeData) : NodeVal<H>(hash, source, timestamp, data)

class Leaf<out H : Hash?>(hash: H, val parent: Node<Hash>, source: DbUuid, timestamp: Long, data: NodeData) : NodeVal<H>(hash, source, timestamp, data)

class Merge<out H : Hash?>(hash: H, val parent1: Node<Hash>, val parent2: Node<Hash>, source: DbUuid, timestamp: Long, data: NodeData) :
        NodeVal<H>(hash, source, timestamp, data)

class Graph(private val resolve: (NodeRef) -> NodeVal<Hash>?) {

    companion object {

        /**
         * Finds set of {@code NodeRef}'s reachable from given node
         */
        fun refs(root: Node<Hash>): Set<NodeRef> {
            return when (root) {
                is NodeRef -> setOf(root)
                is Leaf<Hash> -> refs(root.parent)
                is Merge<Hash> -> refs(root.parent1) + refs(root.parent2)
                is Root<Hash> -> setOf()
            }
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

    fun findSubgraph(n: Node<Hash>, sgRootSource: DbUuid): Node<Hash> = when {
        n is NodeRef -> resolve(n).let { findSubgraph(it!!, sgRootSource) }
        n is NodeVal && n.source == sgRootSource -> NodeRef(n.hash)
        n is Leaf -> {
            val parent = findSubgraph(n.parent, sgRootSource)
            Leaf(n.hash, parent, n.source, n.timestamp, n.data)
        }
        n is Merge -> {
            val parent1 = findSubgraph(n.parent1, sgRootSource)
            val parent2 = findSubgraph(n.parent2, sgRootSource)
            Merge(n.hash, parent1, parent2, n.source, n.timestamp, n.data)
        }
        else -> throw AssertionError("Should never happen")
    }

    fun resolveNode(n: Node<Hash>) = when (n) {
        is NodeVal<Hash> -> n
        is NodeRef -> resolve(n) ?: throw QBitException("Corrupted graph, could not resolve $n")
    }

}

