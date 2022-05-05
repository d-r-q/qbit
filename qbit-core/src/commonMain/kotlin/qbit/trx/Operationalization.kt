package qbit.trx

import qbit.api.QBitException
import qbit.api.model.Attr
import qbit.api.model.DataType
import qbit.api.model.Eav
import qbit.index.InternalDb

fun operationalize(db: InternalDb, facts: List<Eav>): List<Eav> {
    return facts.mapNotNull {
        val attr = db.attr(it.attr)!!
        val dataType = DataType.ofCode(attr.type)!!
        when {
            dataType.isCounter() -> operationalizeCounter(db, it, attr)
            dataType.isRegister() -> operationalizeRegister(db, it, attr)
            else -> it
        }
    }
}

private fun operationalizeCounter(db: InternalDb, fact: Eav, attr: Attr<*>): Eav? {
    val previous = db.pullEntity(fact.gid)?.tryGet(attr)
    return if (previous != null) {
        if(previous != fact.value) {
            Eav(
                fact.gid,
                fact.attr,
                if (fact.value is Byte) fact.value - (previous as Number).toByte()
                else if (fact.value is Int) fact.value - (previous as Number).toInt()
                else if (fact.value is Long) fact.value - (previous as Number).toLong()
                else throw QBitException("Unexpected counter value type for $fact")
            )
        } else null
    } else fact
}

private fun operationalizeRegister(db: InternalDb, fact: Eav, attr: Attr<*>): Eav? {
    val previous = db.pullEntity(fact.gid)?.tryGet(attr)
    return if (fact.value != previous) fact else null
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