package qbit

import qbit.mapping.attrName
import qbit.mapping.destruct
import qbit.mapping.schema
import qbit.model.Attr2
import qbit.model.Gid
import qbit.trx.EmptyIterator


data class User(val id: Long?, val externalId: Int, val name: String, val nicks: List<String>) {

    fun toFacts() =
            destruct(this, schemaMap::get, EmptyIterator)

}


val testSchema = schema {
    entity(User::class) {
        unique(it::externalId)
    }
}

private val eids = Gid(2, 0).nextEids()
val schemaMap: Map<String, Attr2<*>> = testSchema
        .map { it.name to it.id(eids.next()) }
        .toMap()

object Users {

    val extId = schemaMap.getValue(User::class.attrName(User::externalId))
    val name = schemaMap.getValue(User::class.attrName(User::name))
    val nicks = schemaMap.getValue(User::class.attrName(User::nicks))

}

val eCodd = User( 101, 1, "Edgar Codd", listOf("mathematician", "tabulator"))
val pChen = User(102, 2, "Peter Chen", listOf("unificator"))
val mStonebreaker = User(103, 3, "Michael Stonebreaker", listOf("The DBMS researcher"))
val eBrewer = User(104, 4, "Eric Brewer", listOf("Big Data"))
