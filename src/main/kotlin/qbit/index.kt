package qbit

import java.util.*

open class FactPattern(
        open val entityId: EID? = null,
        open val attribute: String? = null,
        open val time: Long? = null,
        open val value: Any? = null) {

    override fun toString(): String {
        return "FactPattern(entityId=$entityId, attribute=$attribute, time=$time, value=$value)"
    }
}

class StoredFact(
        entityId: EID,
        attribute: String,
        time: Long,
        value: Any) : FactPattern(entityId, attribute, time, value)

class Index(
        val avet: TreeSet<FactPattern> = TreeSet(avetCmp),
        val eavt: TreeSet<FactPattern> = TreeSet(eavtCmp)) {

    fun add(facts: List<StoredFact>): Index {
        val newEavt = TreeSet<FactPattern>(eavtCmp)
        newEavt.addAll(eavt)

        val distinctFacts = facts
                .sortedWith(kotlin.Comparator(eatvCmp).reversed())
                .distinctBy { it.entityId to it.attribute }
        distinctFacts.forEach { newEavt.remove(FactPattern(it.entityId, it.attribute))}
        newEavt.addAll(distinctFacts)


        val newAvet = TreeSet<FactPattern>(avetCmp)
        newAvet.addAll(newEavt)

        return Index(newAvet, newEavt)
    }

    fun add(fact: StoredFact): Index {
        return add(listOf(fact))
    }

    fun entityById(eid: EID): Map<String, Any>? {
        try {
            val facts = eavt.subSet(FactPattern(entityId = eid), FactPattern(entityId = eid.next()))
            return facts
                    .filter { it.entityId == eid }
                    .groupBy { it.attribute!! }
                    .mapValues { it.value.last().value!! }
                    .takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            throw QBitException(cause = e)
        }
    }

    private fun EID.next() = EID(this.iid, this.eid + 1)

    fun entitiesByAttr(attr: String): Set<EID> {
        return avet.tailSet(FactPattern(attribute = attr))
                .takeWhile { it.attribute == attr }
                .map { it.entityId!! }
                .toSet()
    }
    fun entitiesByAttrVal(attr: String, value: Any?): Set<EID> {
        return avet.tailSet(FactPattern(attribute = attr, value = value))
                .takeWhile { it.attribute == attr && it.value == value }
                .map { it.entityId!! }
                .toSet()
    }

}

val eavtCmp: (FactPattern, FactPattern) -> Int = cmp(FactPattern::entityId, FactPattern::attribute, FactPattern::value, FactPattern::time)

val avetCmp: (FactPattern, FactPattern) -> Int = cmp(FactPattern::attribute, FactPattern::value, FactPattern::entityId, FactPattern::time)

val eatvCmp: (FactPattern, FactPattern) -> Int = cmp(FactPattern::entityId, FactPattern::attribute, FactPattern::time, FactPattern::value)

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
    return if (v1 != null && v2 != null) {
        val type = DataType.of<Any>(v1) ?: throw IllegalArgumentException("Unsupported type: $v1")
        type.compare(v1, v2)
    } else if (v1 == null && v2 != null) {
        return -1
    } else if (v1 != null && v2 == null) {
        return 1
    } else {
        0
    }
}
