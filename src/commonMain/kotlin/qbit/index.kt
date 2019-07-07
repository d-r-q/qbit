package qbit

import qbit.collections.firstMatchIdx
import qbit.collections.subList
import qbit.model.EID

typealias RawEntity = Pair<EID, List<Fact>>

private fun loadFacts(graph: Graph, head: NodeVal<Hash>): List<RawEntity> {
    val entities = HashMap<EID, List<Fact>>()
    val tombstones = HashSet<EID>()
    var n: NodeVal<Hash>? = head
    while (n != null) {
        val (removed, toAdd) = n.data.trx.partition { it.attr == tombstone.str() }
        tombstones += removed.map { it.eid }.toSet()
        toAdd
                .filterNot { tombstones.contains(it.eid) || entities.containsKey(it.eid) }
                .groupBy { it.eid }
                .forEach {
                    entities[it.key] = it.value
                }
        n = when (n) {
            is Root -> null
            is Leaf -> graph.resolveNode(n.parent)
            is Merge -> graph.resolveNode(n.parent1)
        }
    }
    return entities.entries
            .map { it.key to it.value }
}

fun Index(graph: Graph, head: NodeVal<Hash>): Index {
    return Index().add(loadFacts(graph, head))
}

fun Index(entities: List<RawEntity>): Index =
        Index().add(entities)

fun eidPattern(eid: EID) = { other: Fact -> other.eid.compareTo(eid) }

fun attrPattern(attr: String) = { fact: Fact -> fact.attr.compareTo(attr) }

fun valuePattern(value: Any) = { fact: Fact -> compareValues(fact.value, value) }

fun attrValuePattern(attr: String, value: Any) = composeComparable(attrPattern(attr), valuePattern(value))

fun composeComparable(vararg cmps: (Fact) -> Int) = { fact: Fact ->
    cmps.asSequence()
            .map { it(fact) }
            .dropWhile { it == 0 }
            .firstOrNull() ?: 0
}

val aveCmp = Comparator<Fact> { o1, o2 ->

    var res = o1.attr.compareTo(o2.attr)
    if (res == 0) {
        res = compareValues(o1.value, o2.value)
    }
    if (res == 0) {
        res = o1.eid.compareTo(o2.eid)
    }
    res
}

fun compareValues(v1: Any, v2: Any): Int {
    @Suppress("UNCHECKED_CAST")
    return (v1 as Comparable<Any>).compareTo(v2)
}

class Index(
        val entities: Map<EID, RawEntity> = HashMap(),
        private val index: List<Fact> = ArrayList()
) {

    fun addFacts(facts: List<Fact>): Index {
        val entities = facts
                .groupBy { it.eid }
                .map { it.key to it.value }
        return add(entities)
    }

    fun add(entities: List<RawEntity>): Index {
        val newEntities = HashMap(this.entities)
        val newIndex = ArrayList(index)

        for (e in entities) {
            val prev = if (e.second[0].attr != tombstone.str()) {
                newEntities.put(e.first, e)
            } else {
                newEntities.remove(e.first)
            }
            if (prev != null) {
                newIndex.removeAll(prev.second)
            }
            newIndex.addAll(e.second.filter { it.value is Comparable<*> })
        }

        return Index(newEntities, newIndex.sortedWith(aveCmp))
    }

    fun add(e: RawEntity): Index {
        return add(listOf(e))
    }

    fun entityById(eid: EID): Map<String, List<Any>>? =
            entities[eid]?.second
                    ?.groupBy { it.attr }
                    ?.mapValues {
                        it.value.map { f -> f.value }
                    }

    fun eidsByPred(pred: QueryPred): Sequence<EID> {
        val fromIdx = index.firstMatchIdx {
            if (it.attr == pred.attrName) {
                pred.compareTo(it.value)
            } else {
                it.attr.compareTo(pred.attrName)
            }
        }
        if (fromIdx < 0 || fromIdx == index.size) {
            return emptySequence()
        }
        return index.subList(fromIdx)
                .asSequence()
                .takeWhile { it.attr == pred.attrName && pred.compareTo(it.value) == 0 }
                .map { it.eid }
    }

}
