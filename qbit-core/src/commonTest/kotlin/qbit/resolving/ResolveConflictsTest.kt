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
            val nodes = createNodes(nodesDepth)
            val expectedBase = nodes[2]
            val actualBase = findBaseNode(nodes[0], nodes[1], nodesDepth)
            assertEquals(expectedBase, actualBase)
        }
    }
}