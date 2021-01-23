package qbit.resolving

import qbit.api.gid.Iid
import qbit.api.model.Hash
import qbit.api.model.nullHash
import qbit.api.system.DbUuid
import qbit.serialization.*

fun createNodesOver(nodesDepth: HashMap<Node<Hash>, Int>): List<Node<Hash>> {
    val testDbUuid = DbUuid(Iid(5, 2))
    val testTimestamp = 10L
    val testNodeData = NodeData(emptyArray())
    val root = Root(nullHash, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[root] = 0
    val leaf1 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf1] =  1
    val leaf2 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf2] = 1
    val leaf3 = Leaf(nullHash, leaf1, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf3] = 2
    val merge1 = Merge(nullHash, leaf2, leaf3, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[merge1] = 3
    val leaf4 = Leaf(nullHash, leaf2, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf4] = 2
    val leaf5 = Leaf(nullHash, merge1, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf5] = 4
    return listOf(leaf4, leaf5, leaf2)
}

fun createNodesRoot(nodesDepth: HashMap<Node<Hash>, Int>): Node<Hash>{
    val testDbUuid = DbUuid(Iid(5, 2))
    val testTimestamp = 10L
    val testNodeData = NodeData(emptyArray())
    val root = Root(nullHash, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[root] = 0
    return root
}

fun createNodesEqually(nodesDepth: HashMap<Node<Hash>, Int>):List<Node<Hash>>{
    val testDbUuid = DbUuid(Iid(5, 2))
    val testTimestamp = 10L
    val testNodeData = NodeData(emptyArray())
    val root = Root(nullHash, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[root] = 0
    val leaf1 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf1] =  1
    val leaf2 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf2] = 1
    val leaf3 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf3] = 1
    val leaf4 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf4] = 1
    val leaf5 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf5] = 1
    val leaf6 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf6] = 1
    val merge1 = Merge(nullHash, leaf1, leaf2, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[merge1] = 2
    val leaf7 = Leaf(nullHash, leaf3, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf7] = 3
    val leaf8 = Leaf(nullHash, merge1, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[leaf8] = 3
    val merge2 = Merge(nullHash, leaf7, leaf4, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[merge2] = 3
    val merge3 = Merge(nullHash, leaf8, leaf6, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[merge3] = 4
    val merge4 = Merge(nullHash, leaf5, merge2, testDbUuid, testTimestamp, testNodeData)
    nodesDepth[merge4] = 4
    return listOf(merge3, merge4, root)
}

