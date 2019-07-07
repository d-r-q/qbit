package qbit

import qbit.model.Attr

fun validate(db: Db, facts: List<Fact>, newAttrs: List<Attr<*>> = emptyList()) {
    val newAttrsByName = newAttrs.associateBy { it.str() }
    val factAttrs = facts.map { it.attr to (db.attr(it.attr) ?: newAttrsByName[it.attr]) }.toMap()

    // check for unknown attributes
    val unknownAttrNames = factAttrs
            .filter { it.value == null }
            .map { it.key }
    if (unknownAttrNames.isNotEmpty()) {
        throw QBitException("Unknown attributes: ${unknownAttrNames.joinToString(", ")}")
    }

    // check for uniquiness violation
    // within trx
    facts
            .filter { factAttrs.getValue(it.attr)?.unique ?: false }
            .groupBy { it.attr to it.value }
            .filterValues { it.size > 1 }
            .forEach { throw QBitException("Uniqueness violation for attr ${it.key}") }

    // within db
    facts.forEach {
        val attr: Attr<Any> = factAttrs.getValue(it.attr)!!
        if (attr.unique) {
            val eids = db.query(attrIs(attr, it.value)).map { f -> f.eid }.distinct().toList()
            if (eids.isNotEmpty() && eids != listOf(it.eid)) {
                throw QBitException("Duplicate fact $it for unique attribute")
            }
        }
    }

    // check that scalar attrs has single fact
    facts.groupBy { it.eid to it.attr }
            .filter { !factAttrs.getValue(it.key.second)!!.isList() }
            .forEach {
                if (it.value.size > 1) {
                    throw QBitException("Duplicate facts $it for scalar attribute: ${it.value}")
                }
            }
}