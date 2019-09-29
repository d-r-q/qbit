package qbit

import qbit.mapping.attrName
import qbit.mapping.schema
import qbit.model.Attr2


data class User(val id: Long, val externalId: Int, val name: String)

val testSchema = schema {
    entity(User::class) {
        unique(it::externalId)
    }
}

val schemaMap: Map<String, Attr2<*>> = testSchema.map { it.name to it }.toMap()

object Users {

    val extId = schemaMap.getValue(User::class.attrName(User::externalId))
    val name = schemaMap.getValue(User::class.attrName(User::name))

}
