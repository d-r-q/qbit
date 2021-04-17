package qbit.resolving

import qbit.Attr
import qbit.api.model.Hash
import qbit.platform.runBlocking
import qbit.serialization.NodeVal
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveConflictsTest {

    @Test
    fun findBaseForNodesWithDifferentDepth() {
        runBlocking {
            val result = createNodesOver()
            val nodesDepth = result.first
            val nodes = result.second
            val expectedBase = nodes[2]
            var actualBase = findBaseNode(nodes[0], nodes[1], nodesDepth)
            assertEquals(expectedBase, actualBase)
            actualBase = findBaseNode(nodes[1], nodes[0], nodesDepth)
            assertEquals(expectedBase, actualBase)
        }
    }

    @Test
    fun findBaseForRootNode() {
        runBlocking {
            val result = createNodesRoot()
            val nodesDepth = result.first
            val nodes = result.second
            val expectedBase = result.second[0]
            var actualBase = findBaseNode(nodes[0], nodes[1], nodesDepth)
            assertEquals(expectedBase, actualBase)

            actualBase = findBaseNode(nodes[1], nodes[0], nodesDepth)
            assertEquals(expectedBase, actualBase)
        }
    }

    @Test
    fun findBaseForNodesWithSameDepth() {
        runBlocking {
            val result = createNodesEqually()
            val nodesDepth = result.first
            val nodes = result.second
            val expectedBase = result.second[2]
            val actualBase = findBaseNode(nodes[0], nodes[1], nodesDepth)
            assertEquals(expectedBase, actualBase)
        }
    }

    @JsName("Test_conflict_resolving_if_branch_have_few_changes_for_attribute")
    @Test
    fun `Test conflict resolving if branch have few changes for attribute`() {
        runBlocking {
            val res = createLogsForResolveTest()
            val eavA = res.second[0]
            val logs = res.first
            val diff = logsDiff(logs[0], logs[1], logs[2]) { it as NodeVal<Hash> }
            val result = diff.merge(lastWriterWinsResolve { Attr(null,"") })
            assertEquals(eavA.value, result.second[eavA.gid]!!.second[0].value)
        }
    }
}