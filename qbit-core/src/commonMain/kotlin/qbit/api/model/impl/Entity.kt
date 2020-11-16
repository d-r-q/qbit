 @file:Suppress("UNCHECKED_CAST")

package qbit.api.model.impl

import kotlinx.serialization.Serializable
import qbit.api.gid.Gid
import qbit.api.model.*

fun AttachedEntity(gid: Gid, entries: List<Pair<Attr<Any>, Any>>, resolveGid: (Gid) -> StoredEntity?): StoredEntity {
    return QStoredEntity(gid, entries.map { (a, v) -> a to entity2gid(v) }.toMap(), resolveGid)
}

sealed class QRoEntity(override val gid: Gid) : Entity() {

    override val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = keys.map {
            it eq this[it]
        }.toSet()

    override fun toString() = "Entity(gid = $gid, ${entries.joinToString(",")})"

}

@Serializable
class QTombstone(override val gid: Gid) : Tombstone() {

    override val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = emptySet()

    override val keys: Set<Attr<Any>>
        get() = emptySet()

    override fun <T : Any> tryGet(key: Attr<T>): T? =
        null

    override fun toString(): String {
        return "Tombstone(gid = $gid)"
    }

}

sealed class QEntity(eid: Gid) : QRoEntity(eid)

class DetachedEntity(eid: Gid, map: Map<Attr<Any>, Any>) : QEntity(eid) {

    private val delegate = MapEntity(eid, map)

    override val keys: Set<Attr<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        return delegate.tryGet(key)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetachedEntity) return false

        if (gid != other.gid) return false
        if (delegate != other.delegate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = delegate.hashCode()
        result = 31 * result + gid.hashCode()
        return result
    }

}

class QStoredEntity(override val gid: Gid, map: Map<Attr<Any>, Any>, val resolveGid: (Gid) -> StoredEntity?) :
    StoredEntity() {

    private val delegate = MapEntity(gid, map)

    override val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = delegate.entries

    override val keys: Set<Attr<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        return delegate.tryGet(key)
    }

    override fun pull(gid: Gid): StoredEntity? {
        return resolveGid(gid)
    }

}

private class MapEntity(override val gid: Gid, private val map: Map<Attr<Any>, Any>) : Entity() {

    override val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = map.entries.map { it.key eq it.value }.toSet()

    override val keys: Set<Attr<Any>>
        get() = map.keys

    override fun <T2 : Any> tryGet(key: Attr<T2>): T2? {
        return (map as Map<Attr<T2>, T2>)[key]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapEntity) return false

        if (map.keys.size != other.keys.size) return false
        if (map.keys.any { map[it] != other.map[it] }) return false

        return true
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

}
