package qbit

import qbit.serialization.SimpleSerialization.serializeNode
import java.security.MessageDigest
import java.util.*

data class DbUuid(val iid: IID)

data class Fact(val entityId: EID, val attribute: String, val value: Any)

class NodeData(val trx: Array<out Fact>)

val nullHash = ByteArray(32)

fun hash(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-1").digest(data)

fun hash(parent1: ByteArray, parent2: ByteArray, source: DbUuid, timestamp: Long, data: NodeData) = hash(serializeNode(NodeRef(parent1), NodeRef(parent2), source, timestamp, data))

sealed class Node(val hash: ByteArray)

class NodeRef(hash: ByteArray) : Node(hash) {
    constructor(n: Node) : this(n.hash)
}

sealed class NodeVal(hash: ByteArray, val source: DbUuid, val timestamp: Long, val data: NodeData) : Node(hash)

class Root(source: DbUuid, timestamp: Long, data: NodeData) : NodeVal(hash(nullHash, nullHash, source, timestamp, data), source, timestamp, data)

class Leaf(val parent: Node, source: DbUuid, timestamp: Long, data: NodeData) : NodeVal(hash(nullHash, parent.hash, source, timestamp, data), source, timestamp, data)

class Merge(val parent1: Node, val parent2: Node, source: DbUuid, timestamp: Long, data: NodeData) : NodeVal(hash(parent1.hash, parent2.hash, source, timestamp, data), source, timestamp, data)

class Graph<out T : Node>(val head: T, val source: DbUuid) {

    fun add(data: NodeData): Graph<Leaf> = Graph(Leaf(head, source, System.currentTimeMillis(), data), source)

    fun merge(r: Node): Graph<Merge> = Graph(Merge(head, r, source, System.currentTimeMillis(), NodeData(emptyArray())), source)

    fun append(h: Node): Pair<NodeVal, NodeVal> = when (h) {
        is Root -> throw AssertionError("Could not append $h")
        is Leaf -> {
            val (parent, mergeRoot) = append(h.parent)
            Pair(Leaf(parent, h.source, h.timestamp, h.data), mergeRoot)
        }
        is Merge -> {
            val (parent1, mergeRoot) = append(h.parent1)
            val (parent2, _) = append(h.parent2)
            Pair(Merge(parent1, parent2, h.source, h.timestamp, h.data), mergeRoot)
        }
        is NodeRef -> {
            val localNode = findNode(h.hash, head) ?: throw AssertionError("Could not find parent")
            Pair(localNode, localNode)
        }
    }

    fun walk(walker: (Node) -> Boolean) {
        walkFrom(walker, hashSetOf(), head)
    }

    fun findSubgraph(n: Node, sgRootSource: DbUuid): Node = when {
        n is NodeVal && n.source == sgRootSource -> NodeRef(n.hash)
        n is Leaf -> Leaf(findSubgraph(n.parent, sgRootSource), n.source, n.timestamp, n.data)
        n is Merge -> Merge(findSubgraph(n.parent1, sgRootSource), findSubgraph(n.parent2, sgRootSource), n.source, n.timestamp, n.data)
        else -> throw AssertionError("Should never happen")
    }

    /**
     * sgRoot - root of the subgraph
     */
    fun findSubgraph(n: Node, sgRoot: Node): Node = when {
        n is Leaf && Arrays.equals(n.parent.hash, sgRoot.hash) -> Leaf(NodeRef(n.parent), n.source, n.timestamp, n.data)
        n is Leaf && !Arrays.equals(n.parent.hash, sgRoot.hash) -> findSubgraph(n.parent, sgRoot)
        n is Merge -> Merge(findSubgraph(n.parent1, sgRoot), findSubgraph(n.parent2, sgRoot), n.source, n.timestamp, n.data)
        else -> throw AssertionError("Should never happen, n is $n root is $sgRoot")
    }

    private fun findNode(hash: ByteArray, n: Node): NodeVal? {
        if (n is NodeVal && Arrays.equals(n.hash, hash)) {
            return n
        }
        return when (n) {
            is Leaf -> findNode(hash, n.parent)
            is Merge -> findNode(hash, n.parent1) ?: findNode(hash, n.parent2)
            else -> null
        }
    }

    private fun walkFrom(walker: (Node) -> Boolean, visited: MutableSet<Node>, head: Node): Boolean {
        if (walker(head)) {
            return true
        }
        visited.add(head)
        val toVisit = when (head) {
            is Root -> listOf()
            is Leaf -> listOf(head.parent)
            is Merge -> listOf(head.parent1, head.parent2)
            else -> throw AssertionError("Should never happen")
        }

        toVisit.filter { it !in visited }.forEach { node ->
            if (walkFrom(walker, visited, node)) {
                return true
            }
        }
        return false
    }

}

