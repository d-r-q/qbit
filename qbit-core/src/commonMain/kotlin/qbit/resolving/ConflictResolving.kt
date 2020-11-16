package qbit.resolving

import qbit.api.gid.Gid
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.index.composeComparable
import qbit.serialization.*
import qbit.trx.TrxLog

class ConflictResult(
    internal val result: HasConflictResult,
    internal val conflictsMap: Map<Gid, List<Pair<Eav, Eav>>> = HashMap()
)

internal fun hasConflict(log1: TrxLog, log2: TrxLog): ConflictResult {
    TODO("Not yet implemented")
}

private fun hasConflictEav(eav1: Eav, eav2: Eav): HasConflictResult {
    if (eav1.gid == eav2.gid && eav1.attr == eav2.attr) {
        if (eav1.value == eav2.value) {
            return HasConflictResult.NO_CONFLICT
        }
        return HasConflictResult.CONFLICT
    }
    return HasConflictResult.NO_CONFLICT
}

internal fun resolveConflicts() {
    TODO("Not yet implemented")
}

internal fun findBaseNode(node1: Node<Hash>, node2: Node<Hash>, nodesDepth: Map<Node<Hash>, Int>): Node<Hash>{
     return when{
         node1 == node2 -> node1
         node1 is Root -> node1
         node2 is Root -> node2
         nodesDepth.getValue(node1) > nodesDepth.getValue(node2) -> {
             return when (node1) {
                 is Leaf -> {
                     findBaseNode(node1.parent, node2, nodesDepth)
                 }
                 is Merge -> {
                     val n1 = findBaseNode(node1.parent1, node2, nodesDepth)
                     val n2 = findBaseNode(node1.parent2, node2, nodesDepth)
                     maxOf(n1, n2, compareBy { nodesDepth.getValue(it) })
                 }
                 else -> throw AssertionError("Should never happen, between: $node1 and $node2")
             }
         }
         nodesDepth.getValue(node2) > nodesDepth.getValue(node1) -> {
             return when (node2) {
                 is Leaf -> {
                     findBaseNode(node1, node2.parent, nodesDepth)
                 }
                 is Merge -> {
                     val n1 = findBaseNode(node1, node2.parent1, nodesDepth)
                     val n2 = findBaseNode(node1, node2.parent2, nodesDepth)
                     maxOf(n1, n2, compareBy { nodesDepth.getValue(it) })
                 }
                 else -> throw AssertionError("Should never happen, between: $node1 and $node2")
             }
         }
         nodesDepth.getValue(node1) == nodesDepth.getValue(node2) -> {
             return when{
                 node1 is Leaf && node2 is Leaf -> findBaseNode(node1.parent, node2.parent, nodesDepth)
                 node1 is Leaf && node2 is Merge -> {
                     val n1 = findBaseNode(node1.parent, node2.parent1, nodesDepth)
                     val n2 = findBaseNode(node1.parent, node2.parent2, nodesDepth)
                     maxOf(n1, n2, compareBy { nodesDepth.getValue(it) })
                 }
                 node1 is Merge && node2 is Leaf -> {
                     val n1 = findBaseNode(node1.parent1, node2.parent, nodesDepth)
                     val n2 = findBaseNode(node1.parent2, node2.parent, nodesDepth)
                     maxOf(n1, n2, compareBy { nodesDepth.getValue(it) })
                 }
                 node1 is Merge && node2 is Merge -> {
                     val listBases = ArrayList<Node<Hash>>()
                     listBases.add(findBaseNode(node1.parent1, node2.parent1, nodesDepth))
                     listBases.add(findBaseNode(node1.parent1, node2.parent1, nodesDepth))
                     listBases.add(findBaseNode(node1.parent1, node2.parent1, nodesDepth))
                     listBases.add(findBaseNode(node1.parent1, node2.parent1, nodesDepth))
                     listBases.maxByOrNull { nodesDepth.getValue(it) }!!
                 }
                 else -> throw AssertionError("Should never happen, between: $node1 and $node2")
             }
         }
         else -> throw AssertionError("Should never happen, between: $node1 and $node2")
     }
}