package qbit

import qbit.schema.Attr
import kotlin.reflect.KClass

fun Entity(vararg entries: Pair<Attr<*>, Any>): Entity = MapEntity(mapOf(*entries))

internal fun Entity(eid: EID, entries: Collection<Pair<Attr<*>, Any>>): StoredEntity = StoredMapEntity(eid, mapOf(*entries.toTypedArray()))

interface Entity {

    val keys: Set<Attr<*>>

    operator fun <T : Any> get(key: Attr<T>): T?

    fun <T : Any> set(key: Attr<T>, value: T): Entity

    val entries: Set<Map.Entry<Attr<*>, Any>>
        get() = keys.map {
            object : Map.Entry<Attr<*>, Any> {
                override val key = it
                override val value: Any = this@Entity[it]!!
            }
        }.toSet()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(keyStr: Attr<T>, type: KClass<T>): T? = get(keyStr)?.let {
        if (type == Byte::class && it is Byte) {
            it as T
        } else if (type == Boolean::class && it is Boolean) {
            it as T
        } else if (type.javaObjectType.isAssignableFrom(it.javaClass)) {
            type.java.cast(it)
        } else {
            throw IllegalArgumentException("Could not cast ${it.javaClass} to $type")
        }
    }

    fun toFacts(eid: EID) =
            this.entries.map { (attr, value) -> Fact(eid, attr, value) }

    fun toStored(eid: EID): StoredEntity
}

interface StoredEntity : Entity {

    val eid: EID

    fun toFacts() =
            this.toFacts(eid)

    override fun <T : Any> set(key: Attr<T>, value: T): StoredEntity

}

private class MapEntity(
        private val map: Map<Attr<*>, Any>
) :
        Entity {

    override val keys: Set<Attr<*>>
        get() = map.keys

    override fun <T : Any> get(key: Attr<T>): T? =
            map[key] as T

    override fun <T : Any> set(key: Attr<T>, value: T): Entity {
        val newMap = HashMap(map)
        newMap[key] = value
        return MapEntity(newMap)
    }

    override val entries: Set<Map.Entry<Attr<*>, Any>>
        get() = map.entries

    override fun toStored(eid: EID): StoredEntity =
            StoredMapEntity(eid, map)
}

private class StoredMapEntity(
        override val eid: EID,
        val map: Map<Attr<*>, Any>
) :
        Entity by MapEntity(map),
        StoredEntity {

    override fun <T : Any> set(key: Attr<T>, value: T): StoredEntity {
        val newMap = HashMap(map)
        newMap[key] = value
        return StoredMapEntity(eid, newMap)
    }

}