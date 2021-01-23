package qbit.resolving

import qbit.api.model.Hash
import qbit.platform.runBlocking
import qbit.serialization.Node
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveConflictsTest {

    @Test
    fun findBaseTest(){
        runBlocking {
            val nodesDepth = HashMap<Node<Hash>, Int>()
            var nodes = createNodesOver(nodesDepth)
            var expectedBase = nodes[2]
            var actualBase = findBaseNode(nodes[0], nodes[1], nodesDepth)

            assertEquals(expectedBase, actualBase)
            actualBase = findBaseNode(nodes[1], nodes[0], nodesDepth)
            assertEquals(expectedBase, actualBase)

            actualBase = findBaseNode(nodes[0], nodes[0], nodesDepth)
            assertEquals(nodes[0], actualBase)

            nodesDepth.clear()
            expectedBase = createNodesRoot(nodesDepth)
            actualBase = findBaseNode(nodes[0], expectedBase, nodesDepth)
            assertEquals(expectedBase, actualBase)

            actualBase = findBaseNode(expectedBase, nodes[0], nodesDepth)
            assertEquals(expectedBase, actualBase)

            nodesDepth.clear()
            nodes = createNodesEqually(nodesDepth)
            expectedBase = nodes[2]
            actualBase = findBaseNode(nodes[0], nodes[1], nodesDepth)
            assertEquals(expectedBase, actualBase)
        }
    }

}