@file:Suppress("UNCHECKED_CAST")

package qbit.model

import qbit.QBitException
import qbit.model.tombstone as tsAttr

interface AttrValue<A : Attr<T>, T : Any> {

    val attr: A
    val value: T

    fun toPair() = attr to value

    operator fun component1(): Attr<T> = attr

    operator fun component2(): T = value

}

internal class QbitAttrValue<T : Any>(override val attr: Attr<T>, override val value: T) : AttrValue<Attr<T>, T> {
    override fun toString(): String = "${attr.name}=$value"
}

fun Entity(gid: Gid, vararg entries: Any): Entity {
    return DetachedEntity(gid, entries.filterIsInstance<AttrValue<Attr<Any>, Any>>().map { it.attr to entity2gid(it.value) }.toMap())
}

fun AttachedEntity(gid: Gid, entries: List<Pair<Attr<Any>, Any>>, resolveGid: (Gid) -> StoredEntity?): StoredEntity {
    return QStoredEntity(gid, entries.map { (a, v) -> a to entity2gid(v)}.toMap(), resolveGid)
}

private fun entity2gid(e: Any): Any {
    return when {
        e is Entity -> e.gid
        e is List<*> && !isListOfVals(e as List<Any>) -> e.map(::entity2gid)
        else -> e
    }
}

fun AttachedEntity(gid: Gid, entries: Map<Attr<Any>, Any>, resolveGid: (Gid) -> StoredEntity?): StoredEntity {
    return QStoredEntity(gid, entries.toMap().mapValues { if (it.value is Entity) (it.value as Entity).gid else it.value }, resolveGid)
}

fun Tombstone(eid: Gid): Tombstone = QTombstone(eid)

interface Entity {

    val gid: Gid

    val keys: Set<Attr<Any>>

    operator fun <T : Any> get(key: Attr<T>): T = tryGet(key)
            ?: throw QBitException("Entity $this does not contain value for ${key.name}")

    fun <T : Any> tryGet(key: Attr<T>): T?

    val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = keys.map {
            it eq this[it]
        }.toSet()

}

interface StoredEntity : Entity {

    fun pull(gid: Gid): StoredEntity?

}

interface Tombstone : Entity

internal fun Tombstone.toFacts() = listOf(Fact(this.gid, tsAttr, true))

internal sealed class QRoEntity(override val gid: Gid) : Entity {

    override fun toString() = "Entity(gid = $gid, ${entries.joinToString(",")})"

}

internal class QTombstone(eid: Gid) : QRoEntity(eid), Tombstone {

    override val keys: Set<Attr<Any>>
        get() = emptySet()

    override fun <T : Any> tryGet(key: Attr<T>): T? =
            null

    override fun toString(): String {
        return "Tombstone(gid = $gid)"
    }

}

internal sealed class QEntity(eid: Gid) : QRoEntity(eid), Entity

internal class DetachedEntity(eid: Gid, map: Map<Attr<Any>, Any>) : QEntity(eid) {

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

private class QStoredEntity(gid: Gid, map: Map<Attr<Any>, Any>, val resolveGid: (Gid) -> StoredEntity?) : QEntity(gid), StoredEntity {

    private val delegate = MapEntity(gid, map)

    override val keys: Set<Attr<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        return delegate[key]
    }

    override fun pull(gid: Gid): StoredEntity? {
        return resolveGid(gid)
    }

}

private class MapEntity(override val gid: Gid, private val map: Map<Attr<Any>, Any>) : Entity {

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

internal fun Entity.toFacts(): Collection<Eav> =
        this.entries.flatMap { (attr: Attr<Any>, value) ->
            val type = DataType.ofCode(attr.type)!!
            when {
                type.value() && !attr.list -> listOf(valToFacts(gid, attr, value))
                type.value() && attr.list -> listToFacts(gid, attr, value as List<Any>)
                type.ref() && !attr.list -> listOf(refToFacts(gid, attr, value))
                type.ref() && attr.list -> refListToFacts(gid, attr, value as List<Any>)
                else -> throw AssertionError("Unexpected attr kind: $attr")
            }
        }

private fun <T : Any> valToFacts(eid: Gid, attr: Attr<T>, value: T) =
        Fact(eid, attr, value)

private fun refToFacts(eid: Gid, attr: Attr<Any>, value: Any) =
        Fact(eid, attr, eidOf(value)!!)

private fun listToFacts(eid: Gid, attr: Attr<*>, value: List<Any>) =
        value.map { Fact(eid, attr, it) }

private fun refListToFacts(eid: Gid, attr: Attr<*>, value: List<Any>) =
        value.map { Fact(eid, attr, eidOf(it)!!) }

private fun eidOf(a: Any): Gid? =
        when (a) {
            is Entity -> a.gid
            is Gid -> a
            else -> null
        }

