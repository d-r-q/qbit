package qbit.index

import qbit.collections.firstMatchIdx
import qbit.collections.merge
import qbit.collections.subList
import qbit.model.Fact
import qbit.model.Gid
import qbit.tombstone

typealias RawEntity = Pair<Gid, List<Fact>>

internal fun Index(entities: List<RawEntity>): Index =
        Index().add(entities)

internal fun eidPattern(eid: Gid) = { other: Fact -> other.eid.compareTo(eid) }

internal fun attrPattern(attr: String) = { fact: Fact -> fact.attr.compareTo(attr) }

internal fun valuePattern(value: Any) = { fact: Fact -> compareValues(fact.value, value) }

internal fun attrValuePattern(attr: String, value: Any) = composeComparable(attrPattern(attr), valuePattern(value))

internal fun composeComparable(vararg cmps: (Fact) -> Int) = { fact: Fact ->
    cmps.asSequence()
            .map { it(fact) }
            .dropWhile { it == 0 }
            .firstOrNull() ?: 0
}

internal val aveCmp = Comparator<Fact> { o1, o2 ->

    var res = o1.attr.compareTo(o2.attr)
    if (res == 0) {
        res = compareValues(o1.value, o2.value)
    }
    if (res == 0) {
        res = o1.eid.compareTo(o2.eid)
    }
    res
}

internal fun compareValues(v1: Any, v2: Any): Int {
    @Suppress("UNCHECKED_CAST")
    return (v1 as Comparable<Any>).compareTo(v2)
}

internal class Index(
        val entities: Map<Gid, RawEntity> = HashMap(),
        val index: List<Fact> = ArrayList()
) {

    fun addFacts(facts: List<Fact>): Index =
            addFacts(facts as Iterable<Fact>)

    fun addFacts(facts: Iterable<Fact>): Index {
        val entities = facts
                .groupBy { it.eid }
                .map { it.key to it.value }
        return add(entities)
    }

    fun add(entities: List<RawEntity>): Index {
        val newEntities = HashMap(this.entities)
        val newIndex = ArrayList(index)

        val toRemove = ArrayList<Fact>()
        val toAdd = ArrayList<Fact>()
        for (e in entities) {
            val prev = if (e.second[0].attr != tombstone.name) {
                newEntities.put(e.first, e)
            } else {
                newEntities.remove(e.first)
            }
            if (prev != null) {
                toRemove.addAll(prev.second)
            }
            toAdd.addAll(e.second.filter { it.value is Comparable<*> })
        }
        toAdd.sortWith(aveCmp)

        val filteredIndex = ArrayList<Fact>(toAdd.size)
        var filterIdx = 0
        newIndex.forEach {
            val cmp = if (filterIdx < toRemove.size) {
                aveCmp.compare(it, toRemove[filterIdx])
            } else {
                // add all facts, that are greater, than last to remove fact
                -1
            }
            when {
                cmp < 0 -> filteredIndex.add(it)
                cmp > 0 -> {
                    filterIdx++
                    while (filterIdx < toRemove.size && aveCmp.compare(it, toRemove[filterIdx]) >= 0) {
                        filterIdx++
                    }
                    if (filterIdx == toRemove.size && aveCmp.compare(it, toRemove[filterIdx - 1]) != 0) {
                        filteredIndex.add(it)
                    }
                }
                cmp == 0 -> {
                    // fiter out
                }
            }
        }
        return Index(newEntities, merge(filteredIndex, toAdd, aveCmp))
    }

    fun add(e: RawEntity): Index {
        return add(listOf(e))
    }

    fun entityById(eid: Gid): Map<String, List<Any>>? =
            entities[eid]?.second
                    ?.groupBy { it.attr }
                    ?.mapValues {
                        it.value.map { f -> f.value }
                    }

    fun eidsByPred(pred: QueryPred): Sequence<Gid> {
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
