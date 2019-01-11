package qbit

import qbit.collections.firstMatchIdx
import qbit.collections.subList

typealias RawEntity = Pair<EID, List<Fact>>

private fun loadFacts(graph: Graph, head: NodeVal<Hash>): List<RawEntity> {
    val entities = HashMap<EID, List<Fact>>()
    var n: NodeVal<Hash>? = head
    while (n != null) {
        val toAdd = n.data.trx
                .filterNot { it.deleted || entities.containsKey(it.eid) }
        toAdd
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

val avetCmp = Comparator<Fact> { o1, o2 ->

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
    val type = DataType.of(v1) ?: throw IllegalArgumentException("Unsupported type: $v1")
    return type.compare(v1, v2)
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

    fun add(entites: List<RawEntity>): Index {
        val newEntitiyes = HashMap(entities)
        val newIndex = ArrayList(index)

        for (e in entites) {
            val prev = if (e.second.isNotEmpty()) {
                newEntitiyes.put(e.first, e)
            } else {
                newEntitiyes.remove(e.first)
            }
            if (prev != null) {
                newIndex.removeAll(prev.second)
            }
            newIndex.addAll(e.second)
        }

        return Index(newEntitiyes, newIndex.sortedWith(avetCmp))
    }

    fun add(e: RawEntity): Index {
        return add(e)
    }

    fun entityById(eid: EID): Map<String, Any>? =
            entities[eid]?.second
                    ?.groupBy { it.attr }
                    ?.mapValues {
                        if (it.value.size == 1) {
                            it.value[0].value
                        } else {
                            it.value.map { f -> f.value }
                        }
                    }

    fun eidsByPred(pred: QueryPred): Set<EID> {
        val fromIdx = index.firstMatchIdx {
            if (it.attr == pred.attrName) {
                pred.compareTo(it.value)
            } else {
                it.attr.compareTo(pred.attrName)
            }
        }
        if (fromIdx < 0 || fromIdx == index.size) {
            return emptySet()
        }
        return index.subList(fromIdx)
                .takeWhile { it.attr == pred.attrName && pred.compareTo(it.value) == 0 }
                .map { it.eid }
                .toSet()
    }

}
