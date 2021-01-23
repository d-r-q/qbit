package qbit.resolving

import qbit.api.gid.Gid
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.index.RawEntity
import qbit.platform.currentTimeMillis
import qbit.resolving.model.ConflictGid
import qbit.resolving.model.HasConflictResult
import qbit.serialization.*
import qbit.trx.TrxLog

class ConflictResult(
    internal val result: HasConflictResult,
    internal val conflictsMap: Map<Gid, ConflictGid> = HashMap()
)

internal fun hasConflict(
    base: TrxLog,
    trxLog: TrxLog,
    newLog: TrxLog,
    resolveNode: (Node<Hash>) -> NodeVal<Hash>?
): ConflictResult {
    if (base == trxLog) {
        return ConflictResult(HasConflictResult.NO_CHANGES)
    }
    val conflictsMap = HashMap<Gid, ConflictGid>()
    val nodes = trxLog.nodesSince(base.hash)
    val newNodes = newLog.nodesSince(base.hash)
    for (node in nodes) {
        for (newNode in newNodes) {
            for (eav in node.data.trxes) {
                for (newEav in newNode.data.trxes) {
                    if (hasConflictEav(eav, newEav) == HasConflictResult.CONFLICT) {
                        conflictsMap[eav.gid] = ConflictGid(eav.gid, node, newNode)
                    }
                }
            }
        }
    }
    if (conflictsMap.isEmpty()) {
        return ConflictResult(HasConflictResult.NO_CONFLICT)
    }
    return ConflictResult(HasConflictResult.CONFLICT, conflictsMap)
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

internal fun createRawEntitiesWithoutConflicts(
    base: TrxLog, trxLog: TrxLog,
    newLog: TrxLog,
    conflictResult: ConflictResult
): Map<Gid, RawEntity> {
    val entitiesMap = HashMap<Gid, MutableList<Eav>>()
    val nodes = trxLog.nodesSince(base.hash)
    val newNodes = newLog.nodesSince(base.hash)
    if (conflictResult.result == HasConflictResult.NO_CONFLICT) {
        for (node in nodes + newNodes) {
            for (eav in node.data.trxes) {
                entitiesMap.getOrPut(eav.gid, { mutableListOf() }).add(eav)
            }
        }
    } else {
        val conflictEavs1 = HashMap<Eav, HasConflictResult>()
        val conflictEavs2 = HashMap<Eav, HasConflictResult>()
        sortedEavsByConflict(nodes, conflictResult, conflictEavs1, entitiesMap)
        sortedEavsByConflict(newNodes, conflictResult, conflictEavs2, entitiesMap)
        for (eav1 in conflictEavs1.keys) {
            for (eav2 in conflictEavs2.keys) {
                if (hasConflictEav(eav1, eav2) == HasConflictResult.NO_CONFLICT) {
                    conflictEavs1[eav1] = HasConflictResult.CONFLICT
                    conflictEavs2[eav2] = HasConflictResult.CONFLICT
                    val conflictGid =
                        conflictResult.conflictsMap[eav1.gid] ?: throw AssertionError("Should be not null")
                    if (conflictGid.node1.timestamp > conflictGid.node2.timestamp) {
                        entitiesMap.getOrPut(eav1.gid, { mutableListOf() }).add(eav1)
                    } else {
                        entitiesMap.getOrPut(eav2.gid, { mutableListOf() }).add(eav2)
                    }
                }
            }
            if (conflictEavs1[eav1] == HasConflictResult.NO_CONFLICT) {
                entitiesMap.getOrPut(eav1.gid, { mutableListOf() }).add(eav1)
            }
        }
        conflictEavs2.forEach { entry ->
            if (entry.component2() == HasConflictResult.NO_CONFLICT) {
                entitiesMap.getOrPut(entry.component1().gid, { mutableListOf() }).add(entry.component1())
            }
        }
    }
    return entitiesMap.mapValues { entry -> RawEntity(entry.key, entry.value.toList()) }
}

private fun sortedEavsByConflict(
    nodes: List<NodeVal<Hash>>,
    conflictResult: ConflictResult,
    conflictEavs1: HashMap<Eav, HasConflictResult>,
    entitiesMap: HashMap<Gid, MutableList<Eav>>
) {
    for (node in nodes) {
        for (eav in node.data.trxes) {
            val conflictGid = conflictResult.conflictsMap[eav.gid]
            if (conflictGid != null && (conflictGid.node1 == node || conflictGid.node2 == node)) {
                conflictEavs1[eav] = HasConflictResult.NO_CONFLICT
            } else {
                entitiesMap.getOrPut(eav.gid, { mutableListOf() }).add(eav)
            }
        }
    }
}

internal fun createMergeByEntities(
    dbUuid: DbUuid,
    parent1: NodeVal<Hash>,
    parent2: NodeVal<Hash>,
    entities : Map<Gid, RawEntity>
): Merge<Hash?> {
    var eavs = setOf<Eav>()
    for (entity in entities.values){
        eavs = eavs.union(entity.second)
    }
    return Merge(null, parent1, parent2, dbUuid, currentTimeMillis(), NodeData(eavs.toTypedArray()))
}

fun findBaseNode(node1: Node<Hash>, node2: Node<Hash>, nodesDepth: Map<Node<Hash>, Int>): Node<Hash> {
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