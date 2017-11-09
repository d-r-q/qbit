package qbit

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.*
import java.util.zip.Deflater

data class DbUuid(val iid: IID)

data class Fact(val entityId: EID, val attribute: String, val value: Any)

class NodeData(val trx: Array<out Fact>)

fun render(vararg anys: Any): ByteArray {
    val bytes = anys.map { a ->
        when (a) {
            is N -> a.hash
            is DbUuid -> a.let { byteArray(render(it.iid.value)) }
            is Int -> a.let { byteArrayOf(byteOf(3, it), byteOf(2, it), byteOf(1, it), byteOf(0, it)) }
            is Long -> a.let {
                byteArrayOf(byteOf(7, it), byteOf(6, it), byteOf(5, it), byteOf(4, it),
                        byteOf(3, it), byteOf(2, it), byteOf(1, it), byteOf(0, it))
            }
            is String -> a.toByteArray()
            is NodeData -> byteArray(*a.trx.map { render(it) }.toTypedArray())
            is Fact -> render(a.entityId.value(), a.attribute, a.value)
            is ByteArray -> a
            else -> throw AssertionError("Should never happen, a is $a")
        }
    }
    return byteArray(*bytes.toTypedArray())
}

fun byteArray(vararg parts: ByteArray): ByteArray = ByteArray(parts.sumBy { it.size }) { idx ->
    var ci = idx
    val it = parts.iterator()
    var part = it.next()
    while (ci >= part.size) {
        ci -= part.size
        part = it.next()
    }
    part[ci]
}

fun byteOf(idx: Int, i: Int) = i.shr(8 * idx).and(0xFF).toByte()

fun byteOf(idx: Int, i: Long) = i.shr(8 * idx).and(0xFF).toByte()

fun hash(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-1").digest(data)

fun hash(vararg anys: Any) = hash(render(*anys))

fun compress(data: ByteArray): ByteArray {
    val deflater = Deflater()
    deflater.setInput(data)

    val outputStream = ByteArrayOutputStream(data.size)

    deflater.finish()
    val buffer = ByteArray(1024)
    while (!deflater.finished()) {
        val count = deflater.deflate(buffer)
        outputStream.write(buffer, 0, count)
    }
    outputStream.close()
    val output = outputStream.toByteArray()

    deflater.end()
    return output
}

interface DataHolder {
    val data: NodeData
}

val nullHash = ByteArray(32)

fun render(n: N): ByteArray = when (n) {
    is Root -> render(nullHash, nullHash, n.source, n.timestamp)
    is Node -> render(nullHash, n.parent, n.source, n.timestamp, n.data)
    is Merge -> render(n.parent1, n.parent2, n.source, n.timestamp, n.data)
    is Link -> throw AssertionError("Should never happen, n is $n")
}

sealed class N(val hash: ByteArray, val source: DbUuid, val timestamp: Long) {

    fun link() = Link(hash, source, timestamp)

}

class Root(source: DbUuid, timestamp: Long) : N(hash(nullHash, nullHash, source, timestamp), source, timestamp)

class Node(val parent: N, source: DbUuid, timestamp: Long, override val data: NodeData) : N(hash(nullHash, parent, source, timestamp, data), source, timestamp), DataHolder

class Link(hash: ByteArray, source: DbUuid, timestamp: Long) : N(hash, source, timestamp) {
    constructor(orig: N) : this(orig.hash, orig.source, orig.timestamp)
}

class Merge(val parent1: N, val parent2: N, source: DbUuid, timestamp: Long, override val data: NodeData) : N(hash(parent1, parent2, source, timestamp, data), source, timestamp), DataHolder

class Graph(val head: N, val source: DbUuid) {

    fun add(data: NodeData): Graph {
        return Graph(Node(head, source, System.currentTimeMillis(), data), source)
    }

    fun merge(r: N): Graph {
        return Graph(Merge(head, r, source, System.currentTimeMillis(), NodeData(emptyArray())), source)
    }

    fun append(h: N?): Pair<N, N>? {
        return when {
            h == null -> null
            h is Link -> {
                val localNode = findNode(h.hash, head) ?: throw AssertionError("Could not find parent")
                Pair(localNode, localNode)
            }
            h is Node -> {
                val (parent, mergeRoot) = append(h.parent) ?: throw AssertionError("Could not append $h")
                Pair(Node(parent, h.source, h.timestamp, h.data), mergeRoot)
            }
            h is Merge -> {
                val (parent1, mergeRoot) = append(h.parent1) ?: throw AssertionError("Could not append $h")
                val (parent2, _) = append(h.parent2) ?: throw AssertionError("Could not append $h")
                Pair(Merge(parent1, parent2, h.source, h.timestamp, h.data), mergeRoot)
            }
            else -> throw AssertionError("Should never happen, h is $h")
        }
    }

    fun walk(walker: (N) -> Boolean) {
        walkFrom(walker, hashSetOf(), head)
    }

    fun findSubgraph(n: N, sgRootSource: DbUuid): N {
        return when {
            n.source == sgRootSource -> n.link()
            n is Node -> Node(findSubgraph(n.parent, sgRootSource), n.source, n.timestamp, n.data)
            n is Merge -> Merge(findSubgraph(n.parent1, sgRootSource), findSubgraph(n.parent2, sgRootSource), n.source, n.timestamp, n.data)
            else -> throw AssertionError("Should never happen")
        }
    }

    /**
     * sgRoot - root of the subgraph
     */
    fun findSubgraph(n: N, sgRoot: N): N {
        return when {
            n is Node && Arrays.equals(n.parent.hash, sgRoot.hash) -> Node(Link(n.parent), n.source, n.timestamp, n.data)
            n is Node && !Arrays.equals(n.parent.hash, sgRoot.hash) -> findSubgraph(n.parent, sgRoot)
            n is Merge -> Merge(findSubgraph(n.parent1, sgRoot), findSubgraph(n.parent2, sgRoot), n.source, n.timestamp, n.data)
            else -> throw AssertionError("Should never happen, n is $n root is $sgRoot")
        }
    }

    private fun findNode(hash: ByteArray, n: N): N? {
        if (Arrays.equals(n.hash, hash)) {
            return n
        }
        return when (n) {
            is Node -> findNode(hash, n.parent)
            is Merge -> findNode(hash, n.parent1) ?: findNode(hash, n.parent2)
            else -> null
        }
    }

    private fun walkFrom(walker: (N) -> Boolean, visited: MutableSet<N>, head: N): Boolean {
        if (walker(head)) {
            return true
        }
        visited.add(head)
        val toVisit = when (head) {
            is Root -> listOf()
            is Node -> listOf(head.parent)
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

