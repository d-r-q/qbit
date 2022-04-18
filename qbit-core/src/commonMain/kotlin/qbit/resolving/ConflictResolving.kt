package qbit.resolving

import kotlinx.coroutines.flow.toList
import qbit.api.Instances
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.model.*
import qbit.index.RawEntity
import qbit.serialization.*
import qbit.trx.TrxLog

data class PersistedEav(val eav: Eav, val timestamp: Long, val node: Hash)

data class GidAttr(val gid: Gid, val attr: String)

suspend fun logsDiff(
    baseLog: TrxLog, logA: TrxLog, logB: TrxLog,
    resolveNode: (Node<Hash>) -> NodeVal<Hash>
): LogsDiff {
    val nodesA = logA.nodesAfter(baseLog.head, resolveNode)
    val nodesB = logB.nodesAfter(baseLog.head, resolveNode)
    val writesFromA: Map<GidAttr, List<PersistedEav>> = writtenEntityAttrs(nodesA.toList())
    val writesFromB: Map<GidAttr, List<PersistedEav>> = writtenEntityAttrs(nodesB.toList())
    return LogsDiff(writesFromA, writesFromB)
}

private fun writtenEntityAttrs(nodes: List<NodeVal<Hash>>) =
    nodes.flatMap { n -> n.data.trxes.map { PersistedEav(it, n.timestamp, n.hash) } }
        .groupBy { GidAttr(it.eav.gid, it.eav.attr) }

data class LogsDiff(
    val writesFromA: Map<GidAttr, List<PersistedEav>>,
    val writesFromB: Map<GidAttr, List<PersistedEav>>
) {

    fun reconciliationEntities(resolve: (List<PersistedEav>, List<PersistedEav>) -> List<Eav>): List<RawEntity> {
        val conflictingEavsByGid = writesFromA.keys.intersect(writesFromB.keys).groupBy { it.gid }
        val resolvingEavsByGid = conflictingEavsByGid.mapValues { entry ->
            entry.value.flatMap {
                resolve(writesFromA[it]!!, writesFromB[it]!!)
            }
        }
        return resolvingEavsByGid.values.map { RawEntity(it.first().gid, it) }
    }

    fun logAEntities(): List<RawEntity> {
        val entities: Map<Gid, List<GidAttr>> = writesFromA.keys.groupBy { it.gid }
        return entities.values
            .map { gidAttrs ->
                RawEntity(gidAttrs.first().gid, gidAttrs.map { writesFromA[it]!!.lastByTimestamp()!!.eav })
            }
    }

    private fun List<PersistedEav>.lastByTimestamp() =
        maxByOrNull { it.timestamp }

}

internal fun lastWriterWinsResolve(resolveAttrName: (String) -> Attr<Any>?): (List<PersistedEav>, List<PersistedEav>) -> List<Eav> = { eavsFromA, eavsFromB ->
    require(eavsFromA.isNotEmpty()) { "eavsFromA should be not empty" }
    require(eavsFromB.isNotEmpty()) { "eavsFromB should be not empty" }

    val attr = resolveAttrName(eavsFromA[0].eav.attr)
        ?: throw IllegalArgumentException("Cannot resolve ${eavsFromA[0].eav.attr}")

    when {
        // temporary dirty hack until crdt counter or custom resolution strategy support is implemented
        attr == Instances.nextEid -> listOf((eavsFromA + eavsFromB).maxByOrNull { it.eav.value as Int }!!.eav)
        attr.list -> (eavsFromA + eavsFromB).map { it.eav }.distinct()
        else -> listOf((eavsFromA + eavsFromB).maxByOrNull { it.timestamp }!!.eav)
    }
}

internal fun crdtResolve(
    resolveEntity: (Gid) -> StoredEntity?,
    resolveAttrName: (String) -> Attr<Any>?
): (List<PersistedEav>, List<PersistedEav>) -> List<Eav> = { eavsFromA, eavsFromB ->
    require(eavsFromA.isNotEmpty()) { "eavsFromA should be not empty" }
    require(eavsFromB.isNotEmpty()) { "eavsFromB should be not empty" }

    val gid = eavsFromA[0].eav.gid
    val attr = resolveAttrName(eavsFromA[0].eav.attr)
        ?: throw IllegalArgumentException("Cannot resolve ${eavsFromA[0].eav.attr}")

    when {
        // temporary dirty hack until crdt counter or custom resolution strategy support is implemented
        attr == Instances.nextEid -> listOf((eavsFromA + eavsFromB).maxByOrNull { it.eav.value as Int }!!.eav)
        attr.list -> (eavsFromA + eavsFromB).map { it.eav }.distinct()
        DataType.ofCode(attr.type)!!.isCounter() -> {
            val latestFromA = eavsFromA.maxByOrNull { it.timestamp }!!.eav.value
            val latestFromB = eavsFromB.maxByOrNull { it.timestamp }!!.eav.value
            val previous = resolveEntity(gid)?.tryGet(attr)

            listOf(
                if (previous != null)
                    Eav(
                        eavsFromA[0].eav.gid,
                        eavsFromA[0].eav.attr,
                        if (previous is Byte && latestFromA is Byte && latestFromB is Byte) latestFromA + latestFromB - previous
                        else if (previous is Int && latestFromA is Int && latestFromB is Int) latestFromA + latestFromB - previous
                        else if (previous is Long && latestFromA is Long && latestFromB is Long) latestFromA + latestFromB - previous
                        else throw QBitException("Unexpected counter value type for eav with gid=$gid, attr=$attr")
                    )
                else
                    Eav(
                        eavsFromA[0].eav.gid,
                        eavsFromA[0].eav.attr,
                        if (latestFromA is Byte && latestFromB is Byte) latestFromA + latestFromB
                        else if (latestFromA is Int && latestFromB is Int) latestFromA + latestFromB
                        else if (latestFromA is Long && latestFromB is Long) latestFromA + latestFromB
                        else throw QBitException("Unexpected counter value type for eav with gid=$gid, attr=$attr")
                    )
            )
        }
        DataType.ofCode(attr.type)!!.isRegister() -> {
            val latestFromA =
                eavsFromA.maxOf { it.timestamp }.let { timestamp -> eavsFromA.filter { it.timestamp == timestamp } }
            val latestFromB =
                eavsFromB.maxOf { it.timestamp }.let { timestamp -> eavsFromB.filter { it.timestamp == timestamp } }

            latestFromA.map { it.eav } + latestFromB.map { it.eav }
        }
        else -> listOf((eavsFromA + eavsFromB).maxByOrNull { it.timestamp }!!.eav)
    }
}

internal fun findBaseNode(node1: Node<Hash>, node2: Node<Hash>, nodesDepth: Map<Hash, Int>): Node<Hash> {
    return when {
        node1 == node2 -> node1
        node1 is Root -> node1
        node2 is Root -> node2
        nodesDepth.getValue(node1.hash) > nodesDepth.getValue(node2.hash) -> {
            findBaseForNodesWithDifferentDepth(node1, node2, nodesDepth)
        }
        nodesDepth.getValue(node2.hash) > nodesDepth.getValue(node1.hash) -> {
            findBaseForNodesWithDifferentDepth(node2, node1, nodesDepth)
        }
        nodesDepth.getValue(node1.hash) == nodesDepth.getValue(node2.hash) -> {
            return when {
                node1 is Leaf && node2 is Leaf -> findBaseNode(node1.parent, node2.parent, nodesDepth)
                node1 is Leaf && node2 is Merge -> {
                    findBaseForDeepEqualsLeafAndMergeNodes(node1, node2, nodesDepth)
                }
                node1 is Merge && node2 is Leaf -> {
                    findBaseForDeepEqualsLeafAndMergeNodes(node2, node1, nodesDepth)
                }
                node1 is Merge && node2 is Merge -> {
                    val listBases = ArrayList<Node<Hash>>()
                    listBases.add(findBaseNode(node1.parent1, node2.parent1, nodesDepth))
                    listBases.add(findBaseNode(node1.parent1, node2.parent2, nodesDepth))
                    listBases.add(findBaseNode(node1.parent2, node2.parent1, nodesDepth))
                    listBases.add(findBaseNode(node1.parent2, node2.parent2, nodesDepth))
                    listBases.maxByOrNull { nodesDepth.getValue(it.hash) }!!
                }
                else -> throw AssertionError("Should never happen, between: $node1 and $node2")
            }
        }
        else -> throw AssertionError("Should never happen, between: $node1 and $node2")
    }
}

private fun findBaseForDeepEqualsLeafAndMergeNodes(
    leaf: Leaf<Hash>,
    merge: Merge<Hash>,
    nodesDepth: Map<Hash, Int>
): Node<Hash> {
    val n1 = findBaseNode(leaf.parent, merge.parent1, nodesDepth)
    val n2 = findBaseNode(leaf.parent, merge.parent2, nodesDepth)
    return maxOf(n1, n2, compareBy { nodesDepth.getValue(it.hash) })
}

private fun findBaseForNodesWithDifferentDepth(
    deepNode: Node<Hash>,
    anotherNode: Node<Hash>,
    nodesDepth: Map<Hash, Int>
): Node<Hash> {
    return when (deepNode) {
        is Leaf -> {
            findBaseNode(deepNode.parent, anotherNode, nodesDepth)
        }
        is Merge -> {
            val n1 = findBaseNode(deepNode.parent1, anotherNode, nodesDepth)
            val n2 = findBaseNode(deepNode.parent2, anotherNode, nodesDepth)
            maxOf(n1, n2, compareBy { nodesDepth.getValue(it.hash) })
        }
        else -> throw AssertionError("Should never happen, between: $deepNode and $anotherNode")
    }
}