package qbit

import qbit.model.Attr
import qbit.model.EID
import qbit.model.IID

data class DbUuid(val iid: IID)

fun Fact(eid: EID, attr: Attr<*>, value: Any) = Fact(eid, attr.str(), value)

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

    fun findSubgraph(n: Node<Hash>, sgRootSource: DbUuid): Node<Hash>? {
        val nv = resolveNode(n)
        return when  {
            nv.source == sgRootSource -> NodeRef(n)
            nv is Root -> null
            nv is Leaf -> {
                val parent = findSubgraph(nv.parent, sgRootSource) ?: return null
                Leaf(n.hash, parent, nv.source, nv.timestamp, nv.data)
            }
            nv is Merge -> {
                val parent1 = findSubgraph(nv.parent1, sgRootSource) ?: return null
                val parent2 = findSubgraph(nv.parent2, sgRootSource) ?: return null
                Merge(n.hash, parent1, parent2, nv.source, nv.timestamp, nv.data)
            }
            else -> throw AssertionError("Should never happen")
        }
    }

    fun resolveNode(n: Node<Hash>) = when (n) {
        is NodeVal<Hash> -> n
        is NodeRef -> resolve(n) ?: throw QBitException("Corrupted graph, could not resolve $n")
    }

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

}

