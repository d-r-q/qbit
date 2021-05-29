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

class TrxGraphTest {

    private fun createNodesGraph(baseIsAfter: Boolean): Pair<Pair<NodeVal<Hash>, NodeVal<Hash>>, List<NodeVal<Hash>>>{
        val testDbUuid = DbUuid(Iid(5, 2))
        val testTimestamp = 10L
        val testNodeData = NodeData(emptyArray())
        val root = Root(nullHash, testDbUuid, testTimestamp, testNodeData)
        val leaf1 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
        val leaf2 = Leaf(nullHash, root, testDbUuid, testTimestamp, testNodeData)
        val leaf3 = Leaf(nullHash, leaf1, testDbUuid, testTimestamp, testNodeData)
        val merge1 = Merge(nullHash, root, leaf2, leaf3, testDbUuid, testTimestamp, testNodeData)
        val leaf4 = Leaf(nullHash, merge1, testDbUuid, testTimestamp, testNodeData)
        return when(baseIsAfter){
            true -> (root to leaf4) to listOf(leaf2, leaf1, leaf3, merge1, leaf4)
            false -> (leaf1 to leaf4) to listOf(leaf3, merge1, leaf4)
        }
    }

    @JsName("Test_nodes_between_when_base_after_merge_base")
    @Test
    fun `Test nodes between when base after merge base`(){
        runBlocking {
            val data = createNodesGraph(true)
            val actualList = flow{impl(data.first.first, data.first.second){node -> node as NodeVal<Hash>} }.toList()
            assertEquals(data.second, actualList)
        }
    }

    @JsName("Test_nodes_between_when_base_before_merge_base")
    @Test
    fun `Test nodes between when base before merge base`(){
        runBlocking {
            val data = createNodesGraph(false)
            val actualList = flow{impl(data.first.first, data.first.second){node -> node as NodeVal<Hash>} }.toList()
            assertEquals(data.second, actualList)
        }
    }

}