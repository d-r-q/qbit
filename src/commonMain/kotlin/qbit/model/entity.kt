@file:Suppress("UNCHECKED_CAST")

package qbit.model

import qbit.Db
import qbit.Fact
import qbit.QBitException

interface AttrValue<out A : Attr2<T>, out T : Any> {

    val attr: A
    val value: T

    fun toPair() = attr to value

    operator fun component1(): Attr2<T> = attr

    operator fun component2(): T = value

}

internal class QbitAttrValue<T : Any>(override val attr: Attr2<T>, override val value:T) : AttrValue<Attr2<T>, T>

fun Entity(vararg entries: AttrValue<Attr2<Any>, Any>): Entity {
    return DetachedEntity(null, entries.map { it.toPair() }.toMap())
}

fun Tombstone(eid: Gid): Tombstone = QTombstone(eid)

internal fun Entity(eid: Gid, entries: Collection<Pair<Attr2<*>, Any>>, db: Db): StoredEntity = AttachedEntity(eid, entries.toMap(), db, false)

interface RoEntity {

    val eid: Gid?

    val keys: Set<Attr2<Any>>

    operator fun <T : Any> get(key: Attr2<T>): T = tryGet(key) ?: throw QBitException("Entity $this does not contain value for ${key.name}")

    fun <T : Any> tryGet(key: Attr2<T>): T?

    val entries: Set<AttrValue<Attr2<Any>, Any>>
        get() = keys.map {
            it eq this[it]
        }.toSet()

}

interface Entity : RoEntity {

    fun <T : Any> with(key: Attr2<T>, value: T): Entity =
            with(key eq value)

    fun with(vararg values: AttrValue<Attr2<*>, *>): Entity

    fun <T : Any> remove(key: Attr2<T>): Entity

}

interface StoredEntity : Entity {

    fun peek(key: Attr2<*>) :Any?

    override fun <T : Any> with(key: Attr2<T>, value: T): StoredEntity =
            with(key eq value)

    override fun with(vararg values: AttrValue<Attr2<*>, *>): StoredEntity

    override fun <T : Any> remove(key: Attr2<T>): StoredEntity
}

interface Tombstone : RoEntity

internal sealed class QRoEntity(override val eid: Gid?) : RoEntity

internal class QTombstone(eid: Gid) : QRoEntity(eid), Tombstone {

    override val keys: Set<Attr2<Any>>
        get() = TODO()

    override fun <T : Any> tryGet(key: Attr2<T>): T? {
        TODO()
    }

}

internal sealed class QEntity(eid: Gid?) : QRoEntity(eid), Entity

internal class DetachedEntity(eid: Gid?, map: Map<Attr2<*>, *>) : QEntity(eid) {

    private val delegate = MapEntity(map) { newMap -> DetachedEntity(eid, newMap) }

    constructor () : this(null, emptyMap<Attr2<*>, Any>())

    constructor(eid: Gid) : this(eid, emptyMap<Attr2<*>, Any>())

    constructor(e: RoEntity) : this(e.eid, e.entries.map { it.toPair() }.toMap())

    constructor(eid: Gid, e: RoEntity) : this(eid, e.entries.map { it.toPair() }.toMap())

    override val keys: Set<Attr2<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr2<T>): T? {
        return delegate.tryGet(key)
    }

    override fun <T : Any> with(key: Attr2<T>, value: T): DetachedEntity {
        return delegate.with(key, value)
    }

    override fun with(vararg values: AttrValue<Attr2<*>, *>): DetachedEntity {
        return delegate.with(*values)
    }

    override fun <T : Any> remove(key: Attr2<T>): DetachedEntity {
        return delegate.remove(key)
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

internal class AttachedEntity(eid: Gid, map: Map<Attr2<*>, *>, val db: Db, val dirty: Boolean) : QEntity(eid), StoredEntity {

    private val delegate = MapEntity(map) { newMap -> AttachedEntity(eid, newMap, db, true) }

    override val keys: Set<Attr2<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr2<T>): T? {
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

    override fun  peek(key: Attr2<*>): Any? =
            delegate.tryGet(key)

    override fun <T : Any> with(key: Attr2<T>, value: T): AttachedEntity {
        if (value == this.tryGet(key)) {
            return this
        }
        return delegate.with(key, value)
    }

    override fun with(vararg values: AttrValue<Attr2<*>, *>): AttachedEntity {
        return delegate.with(*values)
    }

    override fun <T : Any> remove(key: Attr2<T>): AttachedEntity {
        return delegate.remove(key)
    }

}

private class MapEntity<out T : Entity>(private val map: Map<Attr2<*>, *>, private val create: (Map<Attr2<*>, *>) -> T) : Entity {

    override val eid: Gid? = null

    override val keys: Set<Attr2<Any>>
        get() = map.keys

    override fun <T : Any> tryGet(key: Attr2<T>): T? {
        return map[key] as T?
    }

    override fun <V : Any> with(key: Attr2<V>, value: V): T =
            with(key eq value)

    override fun with(vararg values: AttrValue<Attr2<*>, *>): T {
        val newMap = HashMap(map)
        for ((key, value) in values) {
            newMap[key] = value
        }
        return create(newMap)
    }

    override fun <V : Any> remove(key: Attr2<V>): T {
        val newMap = HashMap(map)
        newMap -= key
        return create(newMap)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapEntity<*>) return false

        if (map.keys.size != other.keys.size) return false
        if (map.keys.any { map[it] != other.map[it] }) return false

        return true
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

}

internal fun RoEntity.toFacts(eid: Gid): Collection<Fact> =
        this.entries.flatMap { (attr: Attr2<Any>, value) ->
            val type = DataType.ofCode(attr.type)!!
            when  {
                type.value() && !attr.list -> listOf(valToFacts(eid, attr, value))
                type.value() && attr.list -> listToFacts(eid, attr, value as List<Any>)
                type.ref() && !attr.list -> listOf(refToFacts(eid, attr, value))
                type.ref() && attr.list-> refListToFacts(eid, attr, value as List<Any>)
                else -> throw AssertionError("Unexpected attr kind: $attr")
            }
        }

private fun <T : Any> valToFacts(eid: Gid, attr: Attr2<T>, value: T) =
        Fact(eid, attr, value)

private fun refToFacts(eid: Gid, attr: Attr2<Any>, value: Any) =
        Fact(eid, attr, eidOf(value)!!)

private fun listToFacts(eid: Gid, attr: Attr2<*>, value: List<Any>) =
        value.map { Fact(eid, attr, it) }

private fun refListToFacts(eid: Gid, attr: Attr2<*>, value: List<Any>) =
        value.map { Fact(eid, attr, eidOf(it)!!) }

private fun eidOf(a: Any): Gid? =
        when {
            a is RoEntity && a.eid != null -> a.eid
            a is Gid -> a
            else -> null
        }
