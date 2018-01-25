package qbit

import java.util.*

open class FactPattern(
        open val entityId: EID?,
        open val attribute: String?,
        open val time: Long?,
        open val value: Any?)

class StoredFact(
        override val entityId: EID,
        override val attribute: String,
        override val time: Long,
        override val value: Any) : FactPattern(entityId, attribute, time, value)

class Index(val avet: TreeSet<FactPattern> = TreeSet(::avetCmp)) {

    fun add(facts: List<StoredFact>): Index {
        val newVaet = TreeSet<FactPattern>(::avetCmp)
        newVaet.addAll(avet)
        newVaet.addAll(facts)
        return Index(avet)
    }

    fun add(fact: StoredFact): Index {
        val newVaet = TreeSet<FactPattern>(::avetCmp)
        newVaet.addAll(avet)
        newVaet.add(fact)
        return Index(newVaet)
    }

}

fun avetCmp(f1: FactPattern, f2: FactPattern): Int {
    if (f1.attribute == null || f2.attribute == null) {
        throw IllegalArgumentException("avet could not compare fact patterns with empty value $f1, $f2")
    }
    return f1.attribute!!.compareTo(f2.attribute!!)
            .ifZero { c(f1, f2, FactPattern::value) }
            .ifZero { c(f1, f2, FactPattern::entityId) }
            .ifZero { c(f1, f2, FactPattern::time) }
}

private fun Int.ifZero(body: (Int) -> Int) =
        if (this == 0) body(this)
        else this

private fun <T : Any?> c(f1: FactPattern, f2: FactPattern, g: (FactPattern) -> T): Int {
    val v1 = g(f1)
    val v2 = g(f2)
    return if (v1 != null && v2 != null) {
        cmpA(v1, v2)
    } else {
        0
    }
}

private fun cmpA(v1: Any, v2: Any): Int {
    return cmpI(v1, v2) ?:
            cmpL(v1, v2) ?:
            cmpS(v1, v2) ?:
            cmpE(v1, v2) ?:
            throw IllegalArgumentException("Unsupported values $v1, $v2")
}

private fun cmpI(v1: Any, v2: Any) = c2<Int>(v1, v2)
private fun cmpL(v1: Any, v2: Any) = c2<Long>(v1, v2)
private fun cmpS(v1: Any, v2: Any) = c2<String>(v1, v2)
private fun cmpE(v1: Any, v2: Any) = c2<EID>(v1, v2)

private inline fun <reified T : Comparable<T>> c2(v1: Any, v2: Any) =
        if (v1 is T && v2 is T) v1.compareTo(v2)
        else null

