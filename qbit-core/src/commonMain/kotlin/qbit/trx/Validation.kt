package qbit.trx

import qbit.api.QBitException
import qbit.api.db.attrIs
import qbit.api.model.Attr
import qbit.api.model.DataType
import qbit.api.model.Eav
import qbit.index.InternalDb

fun validate(db: InternalDb, facts: List<Eav>, newAttrs: List<Attr<*>> = emptyList()) {
    val newAttrsByName = newAttrs.associateBy { it.name }
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
        .forEach { throw QBitException("Uniqueness violation for attr ${it.key}, entities: ${it.value.map { f -> f.gid }}") }

    // within db
    facts.forEach {
        val attr: Attr<*> = factAttrs.getValue(it.attr)!!
        if (attr.unique) {
            val eids = db.query(attrIs(attr as Attr<Any>, it.value)).map { f -> f.gid }.distinct().toList()
            if (eids.isNotEmpty() && eids != listOf(it.gid)) {
                throw QBitException("Duplicate fact $it for unique attribute")
            }
        }
    }

    // check that scalar attrs has single fact
    facts.groupBy { it.gid to it.attr }
        .filter { factAttrs.getValue(it.key.second)!!.let { attr -> !attr.list && !DataType.ofCode(attr.type)!!.isRegister() && !DataType.ofCode(attr.type)!!.isSet() } }
        .forEach {
            if (it.value.size > 1) {
                throw QBitException("Duplicate facts $it for scalar attribute: ${it.value}")
            }
        }
}