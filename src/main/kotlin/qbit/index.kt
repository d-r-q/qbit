package qbit

import java.util.*

open class FactPattern(
        open val eid: EID? = null,
        open val attr: String? = null,
        open val time: Long? = null,
        open val value: Any? = null) {

    override fun toString(): String {
        return "FactPattern(eid=$eid, attr=$attr, time=$time, value=$value)"
    }
}

class StoredFact(
        override val eid: EID,
        attr: String,
        time: Long,
        value: Any) : FactPattern(eid, attr, time, value)


fun Index(graph: Graph, head: NodeVal<Hash>): Index {
    val parentIdx =
            when (head) {
                is Root -> Index()
                is Leaf -> Index(graph, graph.resolveNode(head.parent))
                is Merge -> {
                    val idx1 = Index(graph, graph.resolveNode(head.parent1))
                    val idx2 = Index(graph, graph.resolveNode(head.parent2))
                    idx2.add(idx1.eavt.filterIsInstance<StoredFact>().toList())
                }
            }
    return parentIdx.add(head.data.trx.map { StoredFact(it.eid, it.attr, head.timestamp, it.value) })
}

class Index(
        val avet: TreeSet<FactPattern> = TreeSet(avetCmp),
        val eavt: TreeSet<FactPattern> = TreeSet(eavtCmp)) {

    fun add(facts: List<StoredFact>): Index {
        val newEavt = TreeSet<FactPattern>(eavtCmp)
        newEavt.addAll(eavt)

        val distinctFacts = facts
                .sortedWith(kotlin.Comparator(eatvCmp).reversed())
                .distinctBy { it.eid to it.attr }
        distinctFacts.forEach { newEavt.removePattern(FactPattern(it.eid, it.attr))}
        newEavt.addAll(distinctFacts)


        val newAvet = TreeSet<FactPattern>(avetCmp)
        newAvet.addAll(newEavt)

        return Index(newAvet, newEavt)
    }

    private fun TreeSet<FactPattern>.removePattern(pattern: FactPattern) {
        this.removeIf {
            pattern.eid?.equals(it.eid) ?: true &&
                    pattern.attr?.equals(it.attr) ?: true &&
                    pattern.value?.equals(it.value) ?: true &&
                    pattern.time?.equals(it.time) ?: true

        }
    }

    fun add(fact: StoredFact): Index {
        return add(listOf(fact))
    }

    fun entityById(eid: EID): Map<String, Any>? {
        try {
            val facts = eavt.subSet(FactPattern(eid = eid), FactPattern(eid = eid.next()))
            return facts
                    .filter { it.eid == eid }
                    .groupBy { it.attr!! }
                    .mapValues { it.value.last().value!! }
                    .takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    private fun EID.next() = EID(this.iid, this.eid + 1)

    fun factsByAttr(attr: String): List<StoredFact> {
        return avet.tailSet(FactPattern(attr = attr))
                .takeWhile { it.attr == attr }
                .filterIsInstance<StoredFact>()
    }

    fun entitiesByAttr(attr: String): Set<EID> {
        return avet.tailSet(FactPattern(attr = attr))
                .takeWhile { it.attr == attr }
                .map { it.eid!! }
                .toSet()
    }

    fun entitiesByAttrVal(attr: String, value: Any?): Set<EID> {
        return avet.tailSet(FactPattern(attr = attr, value = value))
                .takeWhile { it.attr == attr && it.value == value }
                .map { it.eid!! }
                .toSet()
    }

}

val eavtCmp: (FactPattern, FactPattern) -> Int = cmp(FactPattern::eid, FactPattern::attr, FactPattern::value, FactPattern::time)

val avetCmp: (FactPattern, FactPattern) -> Int = cmp(FactPattern::attr, FactPattern::value, FactPattern::eid, FactPattern::time)

val eatvCmp: (FactPattern, FactPattern) -> Int = cmp(FactPattern::eid, FactPattern::attr, FactPattern::time, FactPattern::value)

fun <T : Comparable<T>> cmp(c1: (FactPattern) -> T?, c2: (FactPattern) -> Any?, c3: (FactPattern) -> Any?, c4: (FactPattern) -> Any?) =
        { f1: FactPattern, f2: FactPattern ->
            if (c1(f1) == null || c1(f2) == null) {
                throw IllegalArgumentException("could not compare $f1 and $f2 with $c1")
            }
            c1(f1)!!.compareTo(c1(f2)!!)
                    .ifZero { c(f1, f2, c2) }
                    .ifZero { c(f1, f2, c3) }
                    .ifZero { c(f1, f2, c4) }
        }

private fun Int.ifZero(body: (Int) -> Int) =
        if (this == 0) body(this)
        else this

private fun <T : Any?> c(f1: FactPattern, f2: FactPattern, g: (FactPattern) -> T): Int {
    val v1 = g(f1)
    val v2 = g(f2)
    return when {
        v1 != null && v2 != null -> {
            val type = DataType.of<Any>(v1) ?: throw IllegalArgumentException("Unsupported type: $v1")
            type.compare(v1, v2)
        }

        v1 == null && v2 != null -> -1
        v1 != null && v2 == null -> 1
        else -> 0
    }
}
