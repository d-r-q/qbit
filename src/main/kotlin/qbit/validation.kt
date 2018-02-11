package qbit

import qbit.schema.Schema


fun validate(schema: Schema, facts: List<Fact>) {
    val factAttrs = facts.map { it to schema.find(it.attribute) }
    val unknownAttrNames = factAttrs
            .filter { it.second == null }
            .map { it.first.attribute }
    if (unknownAttrNames.isNotEmpty()) {
        throw QBitException("Unknown attributes: ${unknownAttrNames.joinToString(", ")}")
    }
}