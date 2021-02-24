package qbit.resolving

import io.ktor.utils.io.core.*
import qbit.api.gid.Gid
import qbit.api.gid.Iid
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.serialization.*
import qbit.storage.MemStorage
import qbit.trx.QTrxLog
import qbit.trx.TrxLog
import kotlin.math.max

private fun getNodeDepthByDepthMap(node: NodeVal<Hash>, nodesDepth: Map<Hash, Int>): Int{
    return when(node){
        is Leaf -> nodesDepth[node.parent.hash]!! + 1
        is Merge -> max(nodesDepth[node.parent1.hash]!!, nodesDepth[node.parent2.hash]!!) + 1
        else -> 0
    }
}

fun createNodesOver(): Pair<HashMap<Hash, Int>, List<Node<Hash>>> {
    val nodesDepth = HashMap<Hash, Int>()
    val testDbUuid = DbUuid(Iid(5, 2))
    val testTimestamp = 10L
    val testNodeData = NodeData(emptyArray())
    val root = Root(Hash(nodesDepth.size.toString().toByteArray()), testDbUuid, testTimestamp, testNodeData)
    nodesDepth[root.hash] = getNodeDepthByDepthMap(root, nodesDepth)
    val leaf1 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf1.hash] = getNodeDepthByDepthMap(leaf1, nodesDepth)
    val leaf2 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf2.hash] = getNodeDepthByDepthMap(leaf2, nodesDepth)
    val leaf3 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), leaf1, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf3.hash] = getNodeDepthByDepthMap(leaf3, nodesDepth)
    val merge1 = Merge(Hash(nodesDepth.size.toString().toByteArray()), leaf2, leaf3, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[merge1.hash] = getNodeDepthByDepthMap(merge1, nodesDepth)
    val leaf4 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), leaf2, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf4.hash] = getNodeDepthByDepthMap(leaf4, nodesDepth)
    val leaf5 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), merge1, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf5.hash] = getNodeDepthByDepthMap(leaf5, nodesDepth)
    return Pair(nodesDepth, listOf(leaf4, leaf5, leaf2))
}

fun createNodesRoot(): Pair<HashMap<Hash, Int>, List<Node<Hash>>> {
    val nodesDepth = HashMap<Hash, Int>()
    val testDbUuid = DbUuid(Iid(5, 2))
    val testTimestamp = 10L
    val testNodeData = NodeData(emptyArray())
    val root = Root(Hash(nodesDepth.size.toString().toByteArray()), testDbUuid, testTimestamp, testNodeData)
    nodesDepth[root.hash] = getNodeDepthByDepthMap(root, nodesDepth)
    val node = Leaf(Hash(nodesDepth.size.toString().toByteArray()), root, testDbUuid, testTimestamp, testNodeData)
    return Pair(nodesDepth , listOf(root, node))
}

fun createNodesEqually(): Pair<HashMap<Hash, Int>, List<Node<Hash>>> {
    val nodesDepth = HashMap<Hash, Int>()
    val testDbUuid = DbUuid(Iid(5, 2))
    val testTimestamp = 10L
    val testNodeData = NodeData(emptyArray())
    val root = Root(Hash(nodesDepth.size.toString().toByteArray()), testDbUuid, testTimestamp, testNodeData)
    nodesDepth[root.hash] = getNodeDepthByDepthMap(root, nodesDepth)
    val leaf1 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf1.hash] = getNodeDepthByDepthMap(leaf1, nodesDepth)
    val leaf2 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf2.hash] = getNodeDepthByDepthMap(leaf2, nodesDepth)
    val leaf3 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf3.hash] = getNodeDepthByDepthMap(leaf3, nodesDepth)
    val leaf4 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf4.hash] = getNodeDepthByDepthMap(leaf4, nodesDepth)
    val leaf5 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf5.hash] = getNodeDepthByDepthMap(leaf5, nodesDepth)
    val leaf6 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf6.hash] = getNodeDepthByDepthMap(leaf6, nodesDepth)
    val merge1 = Merge(Hash(nodesDepth.size.toString().toByteArray()), leaf1, leaf2, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[merge1.hash] = getNodeDepthByDepthMap(merge1, nodesDepth)
    val leaf7 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), leaf3, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf7.hash] = getNodeDepthByDepthMap(leaf7, nodesDepth)
    val leaf8 = Leaf(Hash(nodesDepth.size.toString().toByteArray()), merge1, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf8.hash] = getNodeDepthByDepthMap(leaf8, nodesDepth)
    val merge2 = Merge(Hash(nodesDepth.size.toString().toByteArray()), leaf7, leaf4, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[merge2.hash] = getNodeDepthByDepthMap(merge2, nodesDepth)
    val merge3 = Merge(Hash(nodesDepth.size.toString().toByteArray()), leaf8, leaf6, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[merge3.hash] = getNodeDepthByDepthMap(merge3, nodesDepth)
    val merge4 = Merge(Hash(nodesDepth.size.toString().toByteArray()), leaf5, merge2, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[merge4.hash] = getNodeDepthByDepthMap(merge4, nodesDepth)
    return Pair(nodesDepth ,listOf(merge3, merge4, root))
}

fun createLogsForResolveTest(): Pair<List<TrxLog>, List<Eav>>{
    val testDbUuid = DbUuid(Iid(5, 2))
    val nodesDepth = HashMap<Hash, Int>()
    val root = Root(Hash(ByteArray(1) { 1.toByte() }), testDbUuid, 10L, NodeData(Array(3) { i ->
        Eav(
            Gid(i.toLong()),
            i.toString(),
            i
        )
    }))
    nodesDepth[root.hash] = getNodeDepthByDepthMap(root, nodesDepth)
    val eavA1 = Eav(Gid(2.toLong()), 2.toString(), 2)
    val nodeA1 = Leaf(Hash(ByteArray(1) { 2.toByte() }), root, testDbUuid, 12L, NodeData(Array(1) {eavA1}))
    nodesDepth[nodeA1.hash] = getNodeDepthByDepthMap(nodeA1, nodesDepth)
    val eavA2 = Eav(Gid(2.toLong()), 2.toString(), 4)
    val nodeA2 = Leaf(Hash(ByteArray(1) { 3.toByte() }), nodeA1, testDbUuid, 11L, NodeData(Array(1) {eavA2}))
    nodesDepth[nodeA2.hash] = getNodeDepthByDepthMap(nodeA2, nodesDepth)
    val eavB1 = Eav(Gid(1.toLong()), 1.toString(), 3)
    val nodeB1 = Leaf(Hash(ByteArray(1) { 4.toByte() }), root, testDbUuid, 13L, NodeData(Array(1) {eavB1}))
    nodesDepth[nodeB1.hash] = getNodeDepthByDepthMap(nodeB1, nodesDepth)

    val testStorage = CommonNodesStorage(MemStorage())
    val baseLog = QTrxLog(root, nodesDepth, testStorage, testDbUuid)
    val logA = QTrxLog(nodeA2, nodesDepth, testStorage, testDbUuid)
    val logB = QTrxLog(nodeB1, nodesDepth, testStorage, testDbUuid)
    return Pair(listOf(baseLog, logA, logB), listOf(eavA1,eavB1))
}

