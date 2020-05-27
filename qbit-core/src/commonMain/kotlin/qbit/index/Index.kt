package qbit.index

import qbit.api.db.QueryPred
import qbit.api.gid.Gid
import qbit.api.model.Eav
import qbit.api.tombstone
import qbit.platform.collections.firstMatchIdx
import qbit.platform.collections.merge
import qbit.platform.collections.subList

typealias RawEntity = Pair<Gid, List<Eav>>

fun Index(entities: List<RawEntity>): Index =
        Index().add(entities)

fun eidPattern(eid: Gid) = { other: Eav -> other.gid.compareTo(eid) }

fun attrPattern(attr: String) = { fact: Eav -> fact.attr.compareTo(attr) }

fun valuePattern(value: Any) = { fact: Eav -> compareValues(fact.value, value) }

fun attrValuePattern(attr: String, value: Any) = composeComparable(attrPattern(attr), valuePattern(value))

internal fun composeComparable(vararg cmps: (Eav) -> Int) = { fact: Eav ->
    cmps.asSequence()
            .map { it(fact) }
            .dropWhile { it == 0 }
            .firstOrNull() ?: 0
}

internal val aveCmp = Comparator<Eav> { o1, o2 ->

    var res = o1.attr.compareTo(o2.attr)
    if (res == 0) {
        res = compareValues(o1.value, o2.value)
    }
    if (res == 0) {
        res = o1.gid.compareTo(o2.gid)
    }
    res
}

internal fun compareValues(v1: Any, v2: Any): Int {
    @Suppress("UNCHECKED_CAST")
    return (v1 as Comparable<Any>).compareTo(v2)
}

class Index(
    val entities: Map<Gid, RawEntity> = HashMap(),
    val indices: List<Eav> = ArrayList()
) {

    fun addFacts(facts: List<Eav>): Index =
            addFacts(facts as Iterable<Eav>)

    fun addFacts(facts: Iterable<Eav>): Index {
        val entities = facts
                .groupBy { it.gid }
                .map { it.key to it.value }
        return add(entities)
    }

    fun add(entities: List<RawEntity>): Index {
        val newEntities = HashMap(this.entities)
        val newIndex = ArrayList(indices)

        val toRemove = ArrayList<Eav>()
        val toAdd = ArrayList<Eav>()
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
        toRemove.sortWith(aveCmp)
        toAdd.sortWith(aveCmp)

        val filteredIndex = ArrayList<Eav>(toAdd.size)
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
        return Index(
            newEntities,
            merge(filteredIndex, toAdd, aveCmp)
        )
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
        val fromIdx = indices.firstMatchIdx {
            if (it.attr == pred.attrName) {
                pred.compareTo(it.value)
            } else {
                it.attr.compareTo(pred.attrName)
            }
        }
        if (fromIdx < 0 || fromIdx == indices.size) {
            return emptySequence()
        }
        return indices.subList(fromIdx)
                .asSequence()
                .takeWhile { it.attr == pred.attrName && pred.compareTo(it.value) == 0 }
                .map { it.gid }
    }

}
