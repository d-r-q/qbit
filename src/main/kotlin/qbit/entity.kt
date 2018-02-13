package qbit

import qbit.schema.Attr
import kotlin.reflect.KClass

fun Entity(vararg entries: Pair<Attr<*>, Any>): Entity = MapEntity(mapOf(*entries))

fun Entity(eid: EID, entries: List<Pair<Attr<*>, Any>>): StoredEntity = StoredMapEntity(eid, mapOf(*entries.toTypedArray()))

interface Entity {

    val keys: Set<Attr<*>>

    operator fun get(key: Attr<*>): Any?

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

}

interface StoredEntity : Entity {

    val eid: EID

}

private class MapEntity(
        private val map: Map<Attr<*>, Any>
) :
        Entity,
        Map<Attr<*>, Any> by map {
    override val entries: Set<Map.Entry<Attr<*>, Any>>
        get() = map.entries
}

private class StoredMapEntity(
        override val eid: EID,
        map: Map<Attr<*>, Any>
) :
        Entity by MapEntity(map),
        StoredEntity