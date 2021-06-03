package qbit.serialization

import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import qbit.api.gid.Iid
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.platform.runBlocking
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class TrxGraphTest{

    class GraphDescription(val root: Node<Hash>, val nodes: Map<String,Node<Hash>>)

    private fun createNodesGraph(): GraphDescription {
        val testDbUuid = DbUuid(Iid(5, 2))
        val testTimestamp = 10L
        val testNodeData = NodeData(emptyArray())
        val nodes = HashMap<String,Node<Hash>>()
        val root = Root(Hash(nodes.size.toString().toByteArray()), testDbUuid, testTimestamp, testNodeData)
        nodes["root"] = root
        val leaf1 = Leaf(Hash(nodes.size.toString().toByteArray()), root, testDbUuid, testTimestamp, testNodeData)
        nodes["leafBeforeRootAfterMergeParent"] = leaf1
        val leaf2 = Leaf(Hash(nodes.size.toString().toByteArray()), root, testDbUuid, testTimestamp, testNodeData)
        nodes["leafBeforeRootAndMergeParent"] = leaf2
        val leaf3 = Leaf(Hash(nodes.size.toString().toByteArray()), leaf1, testDbUuid, testTimestamp, testNodeData)
        nodes["mergeParent"] = leaf3
        val merge = Merge(Hash(nodes.size.toString().toByteArray()), root, leaf2, leaf3, testDbUuid, testTimestamp, testNodeData)
        nodes["merge"] = merge
        val leaf4 = Leaf(Hash(nodes.size.toString().toByteArray()), merge, testDbUuid, testTimestamp, testNodeData)
        nodes["tailLeafBeforeMerge"] = leaf4
        return GraphDescription(root, nodes)
    }

    @JsName("Test_nodes_between_when_base_after_merge_base")
    @Test
    fun `Test nodes between when base after merge base`(){
        runBlocking {
            val data = createNodesGraph()
            val actualList = flow{impl(data.root, data.nodes["tailLeafBeforeMerge"]!!){node -> node as NodeVal<Hash>} }.toList()
            val expected = listOf(
                data.nodes["leafBeforeRootAndMergeParent"],
                data.nodes["leafBeforeRootAfterMergeParent"],
                data.nodes["mergeParent"],
                data.nodes["merge"],
                data.nodes["tailLeafBeforeMerge"],
            )
            assertEquals(expected, actualList)
        }
    }

    @JsName("Test_nodes_between_when_base_before_merge_base")
    @Test
    fun `Test nodes between when base before merge base`(){
        runBlocking {
            val data = createNodesGraph()
            val actualList = flow{impl(data.nodes["leafBeforeRootAfterMergeParent"], data.nodes["tailLeafBeforeMerge"]!!){node -> node as NodeVal<Hash>} }.toList()
            val expected = listOf(
                data.nodes["mergeParent"],
                data.nodes["merge"],
                data.nodes["tailLeafBeforeMerge"],
            )
            assertEquals(expected, actualList)
        }
    }

    @JsName("Test_nodes_between_for_all_graph")
    @Test
    fun `Test nodes between for all graph`(){
        runBlocking {
            val data = createNodesGraph()
            val actualList = flow{impl(null, data.nodes["tailLeafBeforeMerge"]!!){node -> node as NodeVal<Hash>} }.toList()
            val expected = listOf(
                data.root,
                data.nodes["leafBeforeRootAndMergeParent"],
                data.nodes["leafBeforeRootAfterMergeParent"],
                data.nodes["mergeParent"],
                data.nodes["merge"],
                data.nodes["tailLeafBeforeMerge"],
            )
            assertEquals(expected, actualList)
        }
    }

}