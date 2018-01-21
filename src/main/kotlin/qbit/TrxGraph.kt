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

class Graph(private val resolve: (String) -> NodeVal?) {

    fun findSubgraph(n: Node, sgRootSource: DbUuid): Node = when {
        n is NodeRef -> resolve(n.hash.toHexString()).let { findSubgraph(it!!, sgRootSource) }
        n is NodeVal && n.source == sgRootSource -> NodeRef(n.hash)
        n is Leaf -> {
            val parent = findSubgraph(n.parent, sgRootSource)
            Leaf(parent, n.source, n.timestamp, n.data)
        }
        n is Merge -> {
            val parent1 = findSubgraph(n.parent1, sgRootSource)
            val parent2 = findSubgraph(n.parent2, sgRootSource)
            Merge(parent1, parent2, n.source, n.timestamp, n.data)
        }
        else -> throw AssertionError("Should never happen")
    }

    /**
     * @param walker nodes handler. Should return `true` if walk should be stopped or `false` otherwise
     */
    fun walk(head: Node, walker: (Node) -> Boolean) {
        walkFrom(walker, hashSetOf(), head)
    }

    private fun walkFrom(walker: (Node) -> Boolean, visited: MutableSet<Node>, head: Node): Boolean {
        if (walker(head)) {
            return true
        }
        visited.add(head)
        val toVisit: List<Node> = when (head) {
            is Root -> listOf()
            is Leaf -> listOf(head.parent)
            is Merge -> listOf(head.parent1, head.parent2)
            is NodeRef -> resolve(head.hash.toHexString()).let { h ->
                when (h) {
                    is Root -> listOf()
                    is Leaf -> listOf(h.parent)
                    is Merge -> listOf(h.parent1, h.parent2)
                    else -> throw AssertionError("Should never happen")
                }
            }
        }

        toVisit.filter { it !in visited }.forEach { node ->
            if (walkFrom(walker, visited, node)) {
                return true
            }
        }
        return false
    }

}

fun ByteArray.toHexString() = this.joinToString("") { Integer.toHexString(it.toInt() and 0xFF) }
