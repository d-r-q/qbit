package qbit

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
            .groupBy { it.attr }
            .filterValues { it.size > 1 }
            .forEach { throw QBitException("Uniqueness violation for attr ${it.key}") }

    facts.forEach {
        val attr = factAttrs[it.attr]!!
        if (attr.unique && db.query(attrIs(attr, it.value)).isNotEmpty()) {
            throw QBitException("Duplicate fact $it for unique attribute")
        }
    }
}