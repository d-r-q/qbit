package qbit.schema

import qbit.*

private val qbitAttrs = Namespace("qbit").subNs("attrs")

val attrName = qbitAttrs["name"]
val attrType = qbitAttrs["type"]
val attrUnique = qbitAttrs["unique"]

val name = Attr(attrName, QString, unique = true)
val type = Attr(attrType, QByte)

private const val nsSep = "."
private const val keySep = "/"

data class Attr(val name: Key, val type: DataType<*>,
                val unique: Boolean = false) : Entity,
        Map<String, Any> by mapOf(attrName.toStr() to name.toStr(),
                attrType.toStr() to type.code,
                attrUnique.toStr() to unique) {

    override fun get(key: String): Any? {
        return when (parseAttrName(key)) {
            attrName -> this.name.toStr()
            attrType -> type.code
            attrUnique -> unique
            else -> null
        }
    }

}


class Schema(db: Db) {

    private val attrs = db.entitiesByAttr(attrName.toStr())
            .map {
                Attr(parseAttrName(it.get(attrName.toStr(), String::class)!!),
                        DataType.ofCode(it.get(attrType.toStr(), Byte::class)!!)!!,
                        it.get(attrUnique.toStr(), Boolean::class)!!)
            }

    fun find(attrName: String): Attr? = attrs
            .firstOrNull { it[qbit.schema.attrName.toStr()] == attrName }

}

private fun Key.toStr() = this.ns.parts.joinToString(nsSep) + keySep + this.name

private fun parseAttrName(keyStr: String): Key {
    val parts = keyStr.split(keySep)
    if (parts.size != 2) {
        throw IllegalArgumentException("Malformed attribute name: $keyStr")
    }
    val (ns, name) = parts
    return Namespace.of(*ns.split(nsSep).toTypedArray())[name]
}
