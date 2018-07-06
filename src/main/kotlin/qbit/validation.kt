package qbit

import qbit.schema.Attr


fun validate(db: Db, facts: List<Fact>) {
    val factAttrs = facts.map { it to db.attr(it.attr) }.toMap()
    val unknownAttrNames = factAttrs
            .filter { it.value == null }
            .map { it.key.attr }
    if (unknownAttrNames.isNotEmpty()) {
        throw QBitException("Unknown attributes: ${unknownAttrNames.joinToString(", ")}")
    }

    facts.forEach {
        val attr: Attr<Any> = factAttrs[it]!! as Attr<Any>
        if (attr.unique && db.query(attrIs(attr, it.value)).isNotEmpty()) {
            throw QBitException("Duplicate fact $it for unique attribute")
        }
    }
}