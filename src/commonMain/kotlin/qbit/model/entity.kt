@file:Suppress("UNCHECKED_CAST")

package qbit.model

import qbit.Db
import qbit.Fact
import qbit.QBitException
import qbit.mapping.gid
import qbit.tombstone as tsAttr

interface AttrValue<A : Attr<T>, T : Any> {

    val attr: A
    val value: T

    fun toPair() = attr to value

    operator fun component1(): Attr<T> = attr

    operator fun component2(): T = value

}

internal class QbitAttrValue<T : Any>(override val attr: Attr<T>, override val value:T) : AttrValue<Attr<T>, T>

fun Entity(gid: Gid, vararg entries: Any): Entity {
    return DetachedEntity(gid, entries.filterIsInstance<AttrValue<Attr<Any>, Any>>().map { it.toPair() }.toMap())
}

fun AttachedEntity(gid: Gid, entries: List<Pair<Attr<Any>, Any>>, db: Db): Entity {
    return AttachedEntity(gid, entries.toMap(), db)
}

fun Tombstone(eid: Gid): Tombstone = QTombstone(eid)

internal fun Entity(eid: Gid, entries: Collection<Pair<Attr<Any>, Any>>): DetachedEntity = DetachedEntity(eid, entries.toMap())

interface Entity {

    val eid: Gid

    val keys: Set<Attr<Any>>

    operator fun <T : Any> get(key: Attr<T>): T = tryGet(key) ?: throw QBitException("Entity $this does not contain value for ${key.name}")

    fun <T : Any> tryGet(key: Attr<T>): T?

    val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = keys.map {
            it eq this[it]
        }.toSet()

}

interface StoredEntity : Entity {

    fun peek(key: Attr<*>) :Any?

}

interface Tombstone : Entity

internal fun Tombstone.toFacts() = listOf(Fact(this.eid, tsAttr, true))

internal sealed class QRoEntity(override val eid: Gid) : Entity

internal class QTombstone(eid: Gid) : QRoEntity(eid), Tombstone {

    override val keys: Set<Attr<Any>>
        get() = TODO()

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        TODO()
    }

}

internal sealed class QEntity(eid: Gid) : QRoEntity(eid), Entity

internal class DetachedEntity(eid: Gid, map: Map<Attr<Any>, Any>) : QEntity(eid) {

    private val delegate = MapEntity(eid, map)

    constructor(eid: Gid) : this(eid, emptyMap<Attr<Any>, Any>())

    constructor(e: Entity) : this(e.eid, e.entries.map { it.toPair() }.toMap())

    constructor(eid: Gid, e: Entity) : this(eid, e.entries.map { it.toPair() }.toMap())

    override val keys: Set<Attr<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        return delegate.tryGet(key)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetachedEntity) return false

        if (eid != other.eid) return false
        if (delegate != other.delegate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = delegate.hashCode()
        result = 31 * result + eid.hashCode()
        return result
    }

}

internal class AttachedEntity(gid: Gid, map: Map<Attr<Any>, Any>, val db: Db) : QEntity(gid), StoredEntity {

    private val delegate = MapEntity(gid, map)

    override val keys: Set<Attr<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        val type = DataType.ofCode(key.type)!!
        return if (type.ref()) {
            if (!type.isList()) {
               db.pull(delegate[key] as Gid) as T?
            } else {
                (delegate[key] as List<Gid>).map { db.pull(it )} as T?
            }
        } else {
            delegate[key]
        }
    }

    override fun  peek(key: Attr<*>): Any? =
            delegate.tryGet(key)

}

private class MapEntity(override val eid: Gid, private val map: Map<Attr<Any>, Any>) : Entity {

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

internal fun Entity.toFacts(): Collection<Fact> =
        this.entries.flatMap { (attr: Attr<Any>, value) ->
            val type = DataType.ofCode(attr.type)!!
            when  {
                type.value() && !attr.list -> listOf(valToFacts(eid, attr, value))
                type.value() && attr.list -> listToFacts(eid, attr, value as List<Any>)
                type.ref() && !attr.list -> listOf(refToFacts(eid, attr, value))
                type.ref() && attr.list-> refListToFacts(eid, attr, value as List<Any>)
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
        when {
            a is Entity -> a.eid
            a is Gid -> a
            else -> null
        }

val Any.tombstone
        get() = Tombstone(this.gid)
