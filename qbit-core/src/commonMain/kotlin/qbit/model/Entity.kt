package qbit.model

import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.AttrValue
import qbit.api.model.isListOfVals

internal fun entity2gid(e: Any): Any {
    @Suppress("UNCHECKED_CAST")
    return when {
        e is Entity -> e.gid
        e is List<*> && !isListOfVals(e as List<Any>) -> e.map(::entity2gid)
        else -> e
    }
}

abstract class Entity internal constructor() {

    abstract val gid: Gid

    abstract val keys: Set<Attr<Any>>

    operator fun <T : Any> get(key: Attr<T>): T = tryGet(key)
            ?: throw NoSuchElementException("Entity $this does not contain value for ${key.name}")

    abstract fun <T : Any> tryGet(key: Attr<T>): T?

    abstract val entries: Set<AttrValue<Attr<Any>, Any>>

}

abstract class StoredEntity internal constructor() : Entity() {

    abstract fun pull(gid: Gid): StoredEntity?

}

abstract class Tombstone internal constructor() : Entity()