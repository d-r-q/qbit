package qbit.resolving

import qbit.api.gid.Gid
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.index.RawEntity
import qbit.serialization.*
import qbit.trx.TrxLog

data class PersistedEav(val eav: Eav, val timestamp: Long, val node: Hash)

data class GidAttr(val gid: Gid, val attr: String)

fun logsDiff(
    baseLog: TrxLog, logA: TrxLog, logB: TrxLog,
    resolveNode: (Node<Hash>) -> NodeVal<Hash>?
): LogsDiff {
    if (baseLog == logA) {
        return LogsDiff.noChanges
    }
    val nodesA = logA.nodesSince(baseLog.hash, resolveNode)
    val nodesB = logB.nodesSince(baseLog.hash, resolveNode)
    val writesFromA: Map<GidAttr, List<PersistedEav>> = writtenEntityAttrs(nodesA)
    val writesFromB: Map<GidAttr, List<PersistedEav>> = writtenEntityAttrs(nodesB)
    return LogsDiff(writesFromA.keys + writesFromB.keys, writesFromA, writesFromB)
}

private fun writtenEntityAttrs(nodes: List<NodeVal<Hash>>) =
    nodes.flatMap { n -> n.data.trxes.map { PersistedEav(it, n.timestamp, n.hash) } }
        .groupBy { GidAttr(it.eav.gid, it.eav.attr) }

data class LogsDiff(
    val allWrites: Set<GidAttr>,
    val writesFromA: Map<GidAttr, List<PersistedEav>>,
    val writesFromB: Map<GidAttr, List<PersistedEav>>
) {

    companion object {
        val noChanges = LogsDiff(emptySet(), emptyMap(), emptyMap())
    }

    fun merge(resolve: (List<PersistedEav>, List<PersistedEav>) -> Eav): Map<Gid, RawEntity> {
        val gidAttrsGroupByGid = (writesFromA.keys + writesFromB.keys).groupBy { it.gid }
        val resolvingEavByGid = gidAttrsGroupByGid.mapValues { entry ->
            entry.value.map {
                resolve(writesFromA.getOrElse(it, { emptyList() }), writesFromB.getOrElse(it, { emptyList() }))
//              Second variant (этот вариант, по идее, правильнее, т.к. не зависит от реализации resolve()
//                when {
//                    writesFromA.containsKey(it) && writesFromA.containsKey(it) -> resolve(
//                        writesFromA[it]!!,
//                        writesFromB[it]!!
//                    )
//                    else -> when {
//                        writesFromA.containsKey(it) -> writesFromA[it]!!.maxByOrNull { eav -> eav.timestamp }!!.eav
//                        writesFromB.containsKey(it) -> writesFromB[it]!!.maxByOrNull { eav -> eav.timestamp }!!.eav
//                        else -> throw AssertionError("Should never happen")
//                    }
//                }
            }
        }
        return resolvingEavByGid.mapValues { entry -> RawEntity(entry.key, entry.value) }
    }

}

internal fun lastWriterWinsResolve(): (List<PersistedEav>, List<PersistedEav>) -> Eav = { eavsFromA, eavsFromB ->
    (eavsFromA + eavsFromB).maxByOrNull { it.timestamp }!!.eav
}

internal fun findBaseNode(node1: Node<Hash>, node2: Node<Hash>, nodesDepth: Map<Hash, Int>): Node<Hash> {
    return when {
        node1 == node2 -> node1
        node1 is Root -> node1
        node2 is Root -> node2
        nodesDepth.getValue(node1.hash) > nodesDepth.getValue(node2.hash) -> {
            return when (node1) {
                is Leaf -> {
                    findBaseNode(node1.parent, node2, nodesDepth)
                }
                is Merge -> {
                    val n1 = findBaseNode(node1.parent1, node2, nodesDepth)
                    val n2 = findBaseNode(node1.parent2, node2, nodesDepth)
                    maxOf(n1, n2, compareBy { nodesDepth.getValue(it.hash) })
                }
                else -> throw AssertionError("Should never happen, between: $node1 and $node2")
            }
        }
        nodesDepth.getValue(node2.hash) > nodesDepth.getValue(node1.hash) -> {
            return when (node2) {
                is Leaf -> {
                    findBaseNode(node1, node2.parent, nodesDepth)
                }
                is Merge -> {
                    val n1 = findBaseNode(node1, node2.parent1, nodesDepth)
                    val n2 = findBaseNode(node1, node2.parent2, nodesDepth)
                    maxOf(n1, n2, compareBy { nodesDepth.getValue(it.hash) })
                }
                else -> throw AssertionError("Should never happen, between: $node1 and $node2")
            }
        }
        nodesDepth.getValue(node1.hash) == nodesDepth.getValue(node2.hash) -> {
            return when {
                node1 is Leaf && node2 is Leaf -> findBaseNode(node1.parent, node2.parent, nodesDepth)
                node1 is Leaf && node2 is Merge -> {
                    val n1 = findBaseNode(node1.parent, node2.parent1, nodesDepth)
                    val n2 = findBaseNode(node1.parent, node2.parent2, nodesDepth)
                    maxOf(n1, n2, compareBy { nodesDepth.getValue(it.hash) })
                }
                node1 is Merge && node2 is Leaf -> {
                    val n1 = findBaseNode(node1.parent1, node2.parent, nodesDepth)
                    val n2 = findBaseNode(node1.parent2, node2.parent, nodesDepth)
                    maxOf(n1, n2, compareBy { nodesDepth.getValue(it.hash) })
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