package qbit


fun validate(db: Db, facts: List<Fact>) {
    val factAttrs = facts.map { it to db.attr(it.attr) }
    val unknownAttrNames = factAttrs
            .filter { it.second == null }
            .map { it.first.attr }
    if (unknownAttrNames.isNotEmpty()) {
        throw QBitException("Unknown attributes: ${unknownAttrNames.joinToString(", ")}")
    }
}