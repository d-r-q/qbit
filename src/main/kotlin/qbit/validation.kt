package qbit

import qbit.schema.Attr

fun validate(db: Db, facts: List<Fact>, newAttrs: List<Attr<*>> = emptyList()) {
    val newAttrsByName = newAttrs.associateBy { it.str() }
    val factAttrs = facts.map { it.attr to (db.attr(it.attr) ?: newAttrsByName[it.attr]) }.toMap()
    val unknownAttrNames = factAttrs
            .filter { it.value == null }
            .map { it.key }
    if (unknownAttrNames.isNotEmpty()) {
        throw QBitException("Unknown attributes: ${unknownAttrNames.joinToString(", ")}")
    }

    facts
            .filter { factAttrs.getValue(it.attr)?.unique ?: false }
            .groupBy { it.attr to it.value }
            .filterValues { it.size > 1 }
            .forEach { throw QBitException("Uniqueness violation for attr ${it.key}") }

    facts.forEach {
        val attr: Attr<Any> = factAttrs.getValue(it.attr)!!
        if (attr.unique) {
            val eids = db.query(attrIs(attr, it.value)).map { f -> f.eid }.distinct().toList()
            if (eids.isNotEmpty() && eids != listOf(it.eid)) {
                throw QBitException("Duplicate fact $it for unique attribute")
            }
        }
    }
}