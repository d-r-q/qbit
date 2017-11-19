package qbit

import qbit.serialization.SimpleSerialization.serializeNode
import java.security.MessageDigest
import java.util.*

data class DbUuid(val iid: IID)

data class Fact(val entityId: EID, val attribute: String, val value: Any)

class NodeData(val trx: Array<out Fact>)

const val HASH_LEN = 20

val nullHash = ByteArray(HASH_LEN)

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

class Graph(private val resolve: (String) -> Try<NodeVal?>) {

    fun append(h: Node): Try<Pair<NodeVal, NodeVal>> = when (h) {
        is Root -> throw AssertionError("Could not append $h")
        is Leaf -> {
            val pair = append(h.parent)
            pair.mapOk { (parent, mergeRoot) -> Pair(Leaf(parent, h.source, h.timestamp, h.data), mergeRoot) }
        }
        is Merge -> {
            val pair1 = append(h.parent1)
            val pair2 = ifOk(pair1) { append(h.parent2) }
            ifOk(pair1, pair2) { (parent1, mergeRoot), (parent2, _) ->
                Pair(Merge(parent1, parent2, h.source, h.timestamp, h.data), mergeRoot)
            }
        }
        is NodeRef -> {
            val localNode = resolve(h.hash.toHexString())
            ifOk(localNode) { l ->
                if (l != null) {
                    ok(Pair(l, l))
                } else {
                    err(AssertionError("Could not resolve node ${h.hash.toHexString()}"))
                }
            }
        }
    }

    fun walk(head: Node, walker: (Node) -> Boolean) {
        walkFrom(walker, hashSetOf(), head)
    }

    fun findSubgraph(n: Node, sgRootSource: DbUuid): Try<Node> = when {
        n is NodeRef -> resolve(n.hash.toHexString()).ifOkTry { findSubgraph(it!!, sgRootSource) }
        n is NodeVal && n.source == sgRootSource -> ok(NodeRef(n.hash))
        n is Leaf -> {
            val parent = findSubgraph(n.parent, sgRootSource)
            parent.mapOk { Leaf(it, n.source, n.timestamp, n.data) }
        }
        n is Merge -> {
            val parent1 = findSubgraph(n.parent1, sgRootSource)
            val parent2 = ifOk(parent1) { findSubgraph(n.parent2, sgRootSource) }
            ifOk(parent1, parent2) { p1, p2 ->
                Merge(p1, p2, n.source, n.timestamp, n.data)
            }
        }
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

    private fun walkFrom(walker: (Node) -> Boolean, visited: MutableSet<Node>, head: Node): Boolean {
        if (walker(head)) {
            return true
        }
        visited.add(head)
        val toVisit: Try<List<Node>> = when (head) {
            is Root -> ok(listOf())
            is Leaf -> ok(listOf(head.parent))
            is Merge -> ok(listOf(head.parent1, head.parent2))
            is NodeRef -> resolve(head.hash.toHexString()).mapOk { h ->
                when (h) {
                    is Root -> listOf<Node>()
                    is Leaf -> listOf(h.parent)
                    is Merge -> listOf(h.parent1, h.parent2)
                    else -> throw AssertionError("Should never happen")
                }
            }
        }

        toVisit.mapOk {
            it.filter { it !in visited }.forEach { node ->
                if (walkFrom(walker, visited, node)) {
                    return true
                }
            }
        }
        return false
    }

}

fun ByteArray.toHexString() = this.joinToString("") { Integer.toHexString(it.toInt() and 0xFF) }
