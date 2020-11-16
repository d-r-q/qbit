package qbit.resolving

import qbit.api.gid.Iid
import qbit.api.model.Hash
import qbit.api.model.nullHash
import qbit.api.system.DbUuid
import qbit.serialization.*

fun createNodes(nodesDepth: HashMap<Node<Hash>, Int>): List<Node<Hash>> {
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