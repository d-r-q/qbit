package qbit.trx

import qbit.api.QBitException
import qbit.api.model.DataType
import qbit.api.model.Eav
import qbit.index.InternalDb

fun operationalize(db: InternalDb, facts: List<Eav>): List<Eav> {
    return facts.map { operationalizeCounter(db, it) }
}

private fun operationalizeCounter(db: InternalDb, fact: Eav): Eav {
    val attr = db.attr(fact.attr)!!
    val dataType = DataType.ofCode(attr.type)!!
    return if (dataType.isCounter()) {
        val previous = db.pullEntity(fact.gid)?.tryGet(attr)
        if (previous != null) {
            Eav(
                fact.gid,
                fact.attr,
                if (previous is Byte && fact.value is Byte) fact.value - previous
                else if (previous is Int && fact.value is Int) fact.value - previous
                else if (previous is Long && fact.value is Long) fact.value - previous
                else throw QBitException("Unexpected counter value type for $fact")
            )
        } else fact
    } else fact
}

fun deoperationalize(db: InternalDb, facts: List<Eav>): List<Eav> {
    return facts.map { deoperationalizeCounter(db, it) }
}

private fun deoperationalizeCounter(db: InternalDb, fact: Eav): Eav {
    val attr = db.attr(fact.attr)
    return if (attr != null && DataType.ofCode(attr.type)!!.isCounter()) {
        val previous = db.pullEntity(fact.gid)?.tryGet(attr)
        if (previous != null) {
            Eav(
                fact.gid,
                fact.attr,
                if (fact.value is Byte) (previous as Number).toByte() + fact.value
                else if (fact.value is Int) (previous as Number).toInt() + fact.value
                else if (fact.value is Long) (previous as Number).toLong() + fact.value
                else throw QBitException("Unexpected counter value type for $fact")
            )
        } else fact
    } else fact
}