package qbit.resolving

import qbit.api.gid.Gid
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.index.RawEntity
import qbit.resolving.model.HasConflictResult
import qbit.serialization.*
import qbit.trx.TrxLog

data class ConflictResult(
    internal val result: HasConflictResult,
    internal val conflictsMap: Map<GidAttr, Pair<PersistedEav, PersistedEav>> = HashMap()
)

data class PersistedEav(val eav: Eav, val timestamp: Long, val node: Hash)

data class GidAttr(val gid: Gid, val attr: String)

internal fun hasConflict(
    base: TrxLog,
    trxLog: TrxLog,
    newLog: TrxLog,
    resolveNode: (Node<Hash>) -> NodeVal<Hash>?
): ConflictResult {
    if (base == trxLog) {
        return ConflictResult(HasConflictResult.NO_CHANGES)
    }
    val baseNode = base.nodesSince(base.hash, resolveNode)
    val nodes = trxLog.nodesSince(base.hash, resolveNode) - baseNode
    val newNodes = newLog.nodesSince(base.hash, resolveNode) - baseNode
    var entityAttrsA: Map<GidAttr, List<PersistedEav>> = nodesToGidAttrMap(nodes)
    var entityAttrsB : Map<GidAttr, List<PersistedEav>> = nodesToGidAttrMap(newNodes)
    val writesIntersection = entityAttrsA.keys.intersect(entityAttrsB.keys)
    if (writesIntersection.isEmpty()) {
        return ConflictResult(HasConflictResult.NO_CONFLICT, emptyMap())
    }
    sortEavsByTimestamp(entityAttrsA)
    sortEavsByTimestamp(entityAttrsB)
    val conflictingWrites = writesIntersection.filter { entityAttrsA[it]!!.last().eav.value != entityAttrsB[it]!!.last().eav.value }
    if (conflictingWrites.isEmpty()) {
        return ConflictResult(HasConflictResult.NO_CONFLICT, emptyMap())
    }
    entityAttrsA = deleteNoConflictGids(entityAttrsA, conflictingWrites)
    entityAttrsB = deleteNoConflictGids(entityAttrsB, conflictingWrites)
    val conflictsMap = entityAttrsA.mapValues { Pair(it.value.last(), entityAttrsB[it.key]!!.last()) }
    return ConflictResult(HasConflictResult.CONFLICT, conflictsMap)
}

private fun deleteNoConflictGids(
    entityAttrsA: Map<GidAttr, List<PersistedEav>>,
    conflictingWrites: List<GidAttr>
): Map<GidAttr, List<PersistedEav>> {
    return entityAttrsA.filter { conflictingWrites.contains(it.key) }
}

private fun sortEavsByTimestamp(entityAttrsA: Map<GidAttr, List<PersistedEav>>) {
    entityAttrsA.forEach { entry -> entry.value.sortedBy { it.timestamp } }
}

private fun nodesToGidAttrMap(nodes: List<NodeVal<Hash>>) =
    nodes.flatMap { n -> n.data.trxes.map { PersistedEav(it, n.timestamp, n.hash) } }
        .groupBy { GidAttr(it.eav.gid, it.eav.attr) }



internal fun createRawEntitiesWithoutConflicts(
    base: TrxLog, trxLog: TrxLog,
    newLog: TrxLog,
    conflictResult: ConflictResult,
    resolveNode: (Node<Hash>) -> NodeVal<Hash>?
): Map<Gid, RawEntity> {
    val entitiesMap = HashMap<Gid, MutableList<Eav>>()
    val baseNode = base.nodesSince(base.hash, resolveNode)
    val nodes = trxLog.nodesSince(base.hash, resolveNode) - baseNode
    val newNodes = newLog.nodesSince(base.hash, resolveNode) - baseNode
    if (conflictResult.result == HasConflictResult.NO_CONFLICT) {
        for (node in nodes + newNodes) {
            for (eav in node.data.trxes) {
                entitiesMap.getOrPut(eav.gid, { mutableListOf() }).add(eav)
            }
        }
    } else {
        val entityEavs = nodes.flatMap { n -> n.data.trxes.asList() }
            .filter { eav -> !conflictResult.conflictsMap.values.map { it.first.eav }.contains(eav) }
        val entityEavsNew = newNodes.flatMap { n -> n.data.trxes.asList() }
            .filter { eav -> !conflictResult.conflictsMap.values.map { it.second.eav }.contains(eav) }
        for (eav in entityEavs + entityEavsNew) {
            entitiesMap.getOrPut(eav.gid, { mutableListOf() }).add(eav)
        }
        for(pairEav in conflictResult.conflictsMap.values){
            if(pairEav.first.timestamp > pairEav.second.timestamp){
                entitiesMap.getOrPut(pairEav.first.eav.gid, { mutableListOf() }).add(pairEav.first.eav)
            } else {
                entitiesMap.getOrPut(pairEav.second.eav.gid, { mutableListOf() }).add(pairEav.second.eav)
            }
        }
    }

    return entitiesMap.mapValues { RawEntity(it.key, it.value.distinctBy { eav -> Pair(eav.attr, eav.value) }.toList()) }
}

internal fun findBaseNode(node1: Node<Hash>, node2: Node<Hash>, nodesDepth: Map<Node<Hash>, Int>): Node<Hash> {
    return when {
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
            return when {
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
                    listBases.add(findBaseNode(node1.parent1, node2.parent2, nodesDepth))
                    listBases.add(findBaseNode(node1.parent2, node2.parent1, nodesDepth))
                    listBases.add(findBaseNode(node1.parent2, node2.parent2, nodesDepth))
                    listBases.maxByOrNull { nodesDepth.getValue(it) }!!
                }
                else -> throw AssertionError("Should never happen, between: $node1 and $node2")
            }
        }
        else -> throw AssertionError("Should never happen, between: $node1 and $node2")
    }
}