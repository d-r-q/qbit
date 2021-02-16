package qbit.resolving

import qbit.platform.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveConflictsTest {

    @Test
    fun findBaseForNodesWithDifferentDepth(){
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
    fun findBaseForRootNode(){
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
    fun findBaseForNodesWithSameDepth(){
        runBlocking {
            val result = createNodesEqually()
            val nodesDepth = result.first
            val nodes = result.second
            val expectedBase = result.second[2]
            val actualBase = findBaseNode(nodes[0], nodes[1], nodesDepth)
            assertEquals(expectedBase, actualBase)
        }
    }

}