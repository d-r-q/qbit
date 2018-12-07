package qbit

import qbit.schema.Attr
import qbit.schema.RefAttr
import kotlin.reflect.KClass

fun Entity(vararg entries: Pair<Attr<*>, Any>): Entity = MapEntity(mapOf(*entries))

internal fun Entity(eid: EID, entries: Collection<Pair<Attr<*>, Any>>): StoredEntity = StoredMapEntity(eid, mapOf(*entries.toTypedArray()), false)

interface Entity {

    val keys: Set<Attr<*>>

    operator fun <T : Any> get(key: Attr<T>): T?

    operator fun get(key: RefAttr): Entity?

    fun <T : Any> set(key: Attr<T>, value: T): Entity

    val entries: Set<Map.Entry<Attr<*>, Any>>
        get() = keys.map {
            object : Map.Entry<Attr<*>, Any> {
                override val key = it
                override val value: Any = this@Entity[it as Attr<Any>]!!
            }
        }.toSet()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(keyStr: Attr<T>, type: KClass<T>): T? = get(keyStr)?.let {
        if (type == Byte::class && it is Byte) {
            it
        } else if (type == Boolean::class && it is Boolean) {
            it
        } else if (type.javaObjectType.isAssignableFrom(it.javaClass)) {
            type.java.cast(it)
        } else {
            throw IllegalArgumentException("Could not cast ${it.javaClass} to $type")
        }
    }

    fun toStored(eid: EID): StoredEntity
}

internal fun Entity.toFacts(eid: EID) =
        this.entries.map { (attr, value) -> Fact(eid, attr, value, false) }


internal fun Entity.toFacts(eid: EID, deleted: Boolean) =
        this.entries.map { (attr, value) -> Fact(eid, attr, value, deleted) }

interface StoredEntity : Entity {

    val eid: EID

    val deleted: Boolean

    override fun <T : Any> set(key: Attr<T>, value: T): StoredEntity

    fun delete(): StoredEntity

}

internal fun StoredEntity.toFacts() =
        this.toFacts(eid, this.deleted)

private class MapEntity(
        private val map: Map<Attr<*>, Any>
) :
        Entity {

    override val keys: Set<Attr<*>>
        get() = map.keys

    override fun <T : Any> get(key: Attr<T>): T? =
            map[key] as T?

    override fun get(key: RefAttr): Entity? =
            map[key] as Entity?

    override fun <T : Any> set(key: Attr<T>, value: T): Entity {
        val newMap = HashMap(map)
        newMap[key] = value
        return MapEntity(newMap)
    }

    override val entries: Set<Map.Entry<Attr<*>, Any>>
        get() = map.entries

    override fun toStored(eid: EID): StoredEntity =
            StoredMapEntity(eid, map, false)
}

private class StoredMapEntity(
        override val eid: EID,
        val map: Map<Attr<*>, Any>,
        override val deleted: Boolean
) :
        Entity by MapEntity(map),
        StoredEntity {

    override fun delete(): StoredEntity =
            StoredMapEntity(eid, map, true)

    override fun <T : Any> set(key: Attr<T>, value: T): StoredEntity {
        if (deleted) {
            throw QBitException("Could not change deleted entity")
        }
        val newMap = HashMap(map)
        newMap[key] = value
        return StoredMapEntity(eid, newMap, deleted)
    }

}