package qbit

fun EID.next(step: Int) = EID(this.iid, this.eid + step)

fun eids() =
        generateSequence(EID(0, 0)) { eid -> eid.next(1) }
                .iterator()

fun dbOf(vararg entities: Entity): Db {
    val eids = eids()
    val facts = entities.flatMap { it.toFacts(eids.next()) }
    return IndexDb(Index(facts))
}