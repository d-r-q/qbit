package qbit

import qbit.schema.Schema
import qbit.schema.parseAttrName


fun validate(schema: Schema, facts: List<Fact>) {
    val factAttrs = facts.map { it to schema.find(parseAttrName(it.attr)) }
    val unknownAttrNames = factAttrs
            .filter { it.second == null }
            .map { it.first.attr }
    if (unknownAttrNames.isNotEmpty()) {
        throw QBitException("Unknown attributes: ${unknownAttrNames.joinToString(", ")}")
    }
}