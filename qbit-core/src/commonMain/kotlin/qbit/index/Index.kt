package qbit.index

import qbit.api.db.QueryPred
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.DataType
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.api.tombstone
import qbit.platform.assert
import qbit.platform.collections.firstMatchIdx
import qbit.platform.collections.merge
import qbit.platform.collections.sorted
import qbit.platform.collections.subList

typealias RawEntity = Pair<Gid, List<Eav>>

fun Index(entities: List<RawEntity>): Index =
    Index().add(entities, null, emptyList())

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
    if (v1 is Number && v2 is Number) {
        return v1.toLong().compareTo(v2.toLong())
    }
    @Suppress("UNCHECKED_CAST")
    return (v1 as Comparable<Any>).compareTo(v2)
}

class Index(
    val entities: Map<Gid, RawEntity> = HashMap(),
    val aveIndex: ArrayList<Eav> = ArrayList()
) {

    init {
        assert("ave-ndex should not contain tombstones") {
            aveIndex.none { it.attr == tombstone.name }
        }
    }

    fun addFacts(facts: List<Eav>, hash: Hash? =  null, causalHashes: List<Hash> = emptyList(), resolveAttr: (String) -> Attr<*>? = { null }): Index =
        addFacts(facts as Iterable<Eav>, hash, causalHashes, resolveAttr)

    fun addFacts(facts: Iterable<Eav>, hash: Hash?, causalHashes: List<Hash>, resolveAttr: (String) -> Attr<*>? = { null }): Index {
        val entities = facts
            .groupBy { it.gid }
            .map { it.key to it.value }
        return add(entities, hash, causalHashes, resolveAttr)
    }

    fun add(entities: List<RawEntity>, hash: Hash?, causalHashes: List<Hash>, resolveAttr: (String) -> Attr<*>? = { null }): Index {
        val newEntities = HashMap(this.entities)

        // eavs of removed or updated entities
        val obsoleteEavs = ArrayList<Eav>()
        // eavs of added or updated entities
        val newEavs = ArrayList<Eav>()
        for (e in entities) {
            val (gid, eavs) = e

            val isUpdate = eavs[0].attr != tombstone.name
            val obsoleteEntity = newEntities.get(gid)

            if (isUpdate) {
                val effectiveEavs = ArrayList<Eav>()
                eavs.mapTo(effectiveEavs) { eav ->
                    val attr = resolveAttr(eav.attr)
                    if (attr != null &&  DataType.ofCode(attr.type)!!.isRegister()) {
                        val persistedEav = obsoleteEntity?.second?.firstOrNull { it.attr == eav.attr }
                        if (persistedEav != null) {
                            persistedEav.copy(value = (persistedEav.value as IndexedRegister).indexValue(hash, eav.value, causalHashes))
                        } else {
                            eav.copy(value = IndexedRegister(listOf(Pair(hash, eav.value))))
                        }
                    } else {
                        eav
                    }
                }

                if(obsoleteEntity != null) {
                    effectiveEavs.addAll(obsoleteEntity.second.filter { eav ->
                        val attr = resolveAttr(eav.attr)
                        attr != null &&
                                DataType.ofCode(attr.type)!!.let { it.isCounter() || it.isRegister() } &&
                                eavs.none { it.attr == eav.attr }
                    })
                }

                newEntities.put(gid, RawEntity(gid, effectiveEavs))
                newEavs.addAll(effectiveEavs.filter { it.value is Comparable<*> && it.attr != tombstone.name })
            } else {
                newEntities.remove(gid)
            }

            if (obsoleteEntity != null) {
                obsoleteEavs.addAll(obsoleteEntity.second)
            }
        }

        obsoleteEavs.sortWith(aveCmp)
        newEavs.sortWith(aveCmp)
        val untouchedEavs = this.aveIndex - obsoleteEavs
        val newAveIndex = merge(untouchedEavs, newEavs, aveCmp)

        return Index(newEntities, newAveIndex)
    }

    fun add(e: RawEntity, hash: Hash?, causalHashes: List<Hash>, resolveAttr: (String) -> Attr<*>? = { null }): Index {
        return add(listOf(e), hash, causalHashes, resolveAttr)
    }

    fun entityById(eid: Gid): Map<String, List<Any>>? =
        entities[eid]?.second
            ?.groupBy { it.attr }
            ?.mapValues {
                it.value.map { f -> f.value }
            }

    fun eidsByPred(pred: QueryPred): Sequence<Gid> {
        val fromIdx = aveIndex.firstMatchIdx {
            if (it.attr == pred.attrName) {
                pred.compareTo(it.value)
            } else {
                it.attr.compareTo(pred.attrName)
            }
        }
        if (fromIdx < 0 || fromIdx == aveIndex.size) {
            return emptySequence()
        }
        return aveIndex.subList(fromIdx)
            .asSequence()
            .takeWhile { it.attr == pred.attrName && pred.compareTo(it.value) == 0 }
            .map { it.gid }
    }

}

/**
 * Returns elements of `this`, that aren't exist in `another`.
 * Requires lists to be sorted wtih `aveCmp` to execute filtration in linear time
 */
private operator fun ArrayList<Eav>.minus(another: ArrayList<Eav>): ArrayList<Eav> {
    assert("Left operand should be sorted with aveCmp") { sorted(this, aveCmp) }
    assert("Right operand should be sorted with aveCmp") { sorted(another, aveCmp) }
    val res = ArrayList<Eav>(this.size)
    var filterIdx = 0
    this.forEach {
        val cmp = if (filterIdx < another.size) {
            aveCmp.compare(it, another[filterIdx])
        } else {
            // add all facts, that are greater, than last to remove fact
            -1
        }
        when {
            cmp < 0 -> res.add(it)
            cmp > 0 -> {
                filterIdx++
                while (filterIdx < another.size && aveCmp.compare(it, another[filterIdx]) >= 0) {
                    filterIdx++
                }
                if (aveCmp.compare(it, another[filterIdx - 1]) != 0) {
                    res.add(it)
                }
            }
            cmp == 0 -> {
                // filter out
            }
        }
    }
    return res
}

