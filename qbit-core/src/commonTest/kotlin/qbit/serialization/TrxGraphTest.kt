package qbit.serialization

import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.toList
import qbit.api.gid.Iid
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.platform.runBlocking
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 *     root
 *    /   \
 *   L1   L2
 *   |    |
 *   L3   |
 *    \  /
 *    merge
 *     |
 *    head
 */
const val root = "root"
const val l1 = "L1"
const val l2 = "L2"
const val l3 = "L3"
const val merge = "merge"
const val head = "head"

class TrxGraphTest {

    class GraphDescription(val root: Node<Hash>, val nodes: Map<String, Node<Hash>>) {
        operator fun get(name: String): Node<Hash> =
            nodes[name] ?: throw IllegalArgumentException("Cannot find node for name = $name")
    }

    private fun createNodesGraph(): GraphDescription {
        val testDbUuid = DbUuid(Iid(5, 2))
        val testTimestamp = 10L
        val testNodeData = NodeData(emptyArray())
        val nodes = HashMap<String, Node<Hash>>()
        val rootNode = Root(Hash(nodes.size.toString().toByteArray()), testDbUuid, testTimestamp, testNodeData)
        nodes[root] = rootNode
        val leaf1 = Leaf(Hash(nodes.size.toString().toByteArray()), rootNode, testDbUuid, testTimestamp, testNodeData)
        nodes[l1] = leaf1
        val leaf2 = Leaf(Hash(nodes.size.toString().toByteArray()), rootNode, testDbUuid, testTimestamp, testNodeData)
        nodes[l2] = leaf2
        val leaf3 = Leaf(Hash(nodes.size.toString().toByteArray()), leaf1, testDbUuid, testTimestamp, testNodeData)
        nodes[l3] = leaf3
        val mergeNode = Merge(
            Hash(nodes.size.toString().toByteArray()),
            rootNode,
            leaf2,
            leaf3,
            testDbUuid,
            testTimestamp,
            testNodeData
        )
        nodes[merge] = mergeNode
        val leaf4 = Leaf(Hash(nodes.size.toString().toByteArray()), mergeNode, testDbUuid, testTimestamp, testNodeData)
        nodes[head] = leaf4
        return GraphDescription(rootNode, nodes)
    }

    @JsName("Test_nodes_between_when_base_after_merge_base")
    @Test
    fun `nodesBetween should return both merge branches, when when base is equal to merge base`(): Unit = runBlocking {
        // Given a graph
        val data = createNodesGraph()

        // When nodesBetween merge base and merge descendant are requested
        val actualList = nodesBetween(data.root, data[head]) { node -> node as NodeVal<Hash> }.toList()

        // Then both merge branches, merge node and merge descendand is returned
        val expected = listOf(
            data[l2],
            data[l1],
            data[l3],
            data[merge],
            data[head],
        )
        assertEquals(expected, actualList)
    }

    @JsName("nodesBetween_should_return_only_left_branch_of_merge_when_base_is_in_left_branch")
    @Test
    fun `nodesBetween should return only left branch of merge, when base is in left branch`(): Unit = runBlocking {
        // Given a graph
        val data = createNodesGraph()

        // When nodesBetween node in left branch of merge and head are requested
        val actualList = nodesBetween(data[l1], data[head]) { node -> node as NodeVal<Hash> }.toList()

        // Then only left branch, merge and head is returned
        val expected = listOf(
            data[l3],
            data[merge],
            data[head],
        )
        assertEquals(expected, actualList)
    }

    @JsName("nodesBetween_should_return_full_graph_when_base_isnt_specified")
    @Test
    fun `nodesBetween should return full graph, when base isn't specified`(): Unit = runBlocking {
        // Given a graph
        val data = createNodesGraph()

        // When nodesBetween null and head are requested
        val actualList = nodesBetween(null, data[head]) { node -> node as NodeVal<Hash> }.toList()

        // Then all graph is returned
        val expected = listOf(
            data.root,
            data[l2],
            data[l1],
            data[l3],
            data[merge],
            data[head],
        )
        assertEquals(expected, actualList)
    }

}