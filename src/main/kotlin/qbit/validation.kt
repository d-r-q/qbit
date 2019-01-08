package qbit

import qbit.model.Fact
import qbit.util.QBitException

fun validate(db: Db, facts: List<Fact>) {
    val factAttrs = facts.map { it.attr to db.attr(it.attr) }.toMap()
    val unknownAttrNames = factAttrs
            .filter { it.value == null }
            .map { it.key }
    if (unknownAttrNames.isNotEmpty()) {
        throw QBitException("Unknown attributes: ${unknownAttrNames.joinToString(", ")}")
    }

    facts
            .filter { factAttrs[it.attr]!!.unique }
            .groupBy { it.attr to it.value }
            .filterValues { it.size > 1 }
            .forEach { throw QBitException("Uniqueness violation for attr ${it.key}") }

    facts.forEach {
        val attr = factAttrs[it.attr]!!
        if (attr.unique) {
            val eids = db.query(attrIs(attr, it.value)).map { it.eid }.distinct()
            if (eids.isNotEmpty() && eids != listOf(it.eid)) {
                throw QBitException("Duplicate fact $it for unique attribute")
            }
        }
    }
}