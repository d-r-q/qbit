package qbit.serialization

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import qbit.api.gid.Iid
import qbit.api.model.Hash
import qbit.api.model.nullHash
import qbit.api.system.DbUuid
import qbit.platform.runBlocking
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class TrxGraphTest{

    enum class BaseGraph {
        NONE, AFTER, BEFORE
    }

    private fun createNodesGraph(baseGraph: BaseGraph): Pair<Pair<NodeVal<Hash>?, NodeVal<Hash>>, List<NodeVal<Hash>>>{
        val testDbUuid = DbUuid(Iid(5, 2))
        val testTimestamp = 10L
        val testNodeData = NodeData(emptyArray())
        val root = Root(nullHash, testDbUuid, testTimestamp, testNodeData)
        val leaf1 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
        val leaf2 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
        val leaf3 = Leaf(nullHash, leaf1, testDbUuid, testTimestamp, testNodeData)
        val merge1 = Merge(nullHash, root, leaf2, leaf3, testDbUuid, testTimestamp, testNodeData)
        val leaf4 = Leaf(nullHash, merge1, testDbUuid, testTimestamp, testNodeData)
        return when(baseGraph){
            BaseGraph.AFTER -> (root to leaf4) to listOf(leaf2, leaf1, leaf3, merge1, leaf4)
            BaseGraph.BEFORE -> (leaf1 to leaf4) to listOf(leaf3, merge1, leaf4)
            BaseGraph.NONE -> (null to leaf4) to listOf(root, leaf2, leaf1, leaf3, merge1, leaf4)
        }
    }

    @JsName("Test_nodes_between_when_base_after_merge_base")
    @Test
    fun `Test nodes between when base after merge base`(){
        runBlocking {
            val data = createNodesGraph(BaseGraph.AFTER)
            val actualList = flow{impl(data.first.first, data.first.second){node -> node as NodeVal<Hash>} }.toList()
            assertEquals(data.second, actualList)
        }
    }

    @JsName("Test_nodes_between_when_base_before_merge_base")
    @Test
    fun `Test nodes between when base before merge base`(){
        runBlocking {
            val data = createNodesGraph(BaseGraph.BEFORE)
            val actualList = flow{impl(data.first.first, data.first.second){node -> node as NodeVal<Hash>} }.toList()
            assertEquals(data.second, actualList)
        }
    }

    @JsName("Test_nodes_between_for_all_graph")
    @Test
    fun `Test nodes between for all graph`(){
        runBlocking {
            val data = createNodesGraph(BaseGraph.NONE)
            val actualList = flow{impl(data.first.first, data.first.second){node -> node as NodeVal<Hash>} }.toList()
            assertEquals(data.second, actualList)
        }
    }

}