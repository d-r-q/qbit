package qbit.api.model

import qbit.api.gid.Gid

internal fun entity2gid(e: Any): Any {
    @Suppress("UNCHECKED_CAST")
    return when {
        e is Entity -> e.gid
        e is List<*> && !isListOfVals(e as List<Any>) -> e.map(::entity2gid)
        else -> e
    }
}

interface Entity {

    val gid: Gid

    val keys: Set<Attr<Any>>

    operator fun <T : Any> get(key: Attr<T>): T = tryGet(key)
            ?: throw NoSuchElementException("Entity $this does not contain value for ${key.name}")

    fun <T : Any> tryGet(key: Attr<T>): T?

    val entries: Set<AttrValue<Attr<Any>, Any>>

}

interface StoredEntity : Entity {

    fun pull(gid: Gid): StoredEntity?

}

abstract class Tombstone internal constructor() : Entity