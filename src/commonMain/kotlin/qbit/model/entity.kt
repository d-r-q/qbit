@file:Suppress("UNCHECKED_CAST")

package qbit.model

import qbit.*

interface AttrValue<out A : Attr2<T>, out T : Any> {

    val attr: A
    val value: T

    fun toPair() = attr to value

    operator fun component1(): Attr2<T> = attr

    operator fun component2(): T = value

}

internal class QbitAttrValue<T : Any>(override val attr: Attr2<T>, override val value:T) : AttrValue<Attr2<T>, T>

fun Entity(vararg entries: AttrValue<Attr2<Any>, Any>): Entity<EID?> {
    return DetachedEntity(null, entries.map { it.toPair() }.toMap())
}

fun Tombstone(eid: EID): Tombstone = QTombstone(eid)

internal fun Entity(eid: EID, entries: Collection<Pair<Attr2<*>, Any>>, db: Db): StoredEntity = AttachedEntity(eid, entries.toMap(), db, false)

interface RoEntity<out E : EID?> {

    val eid: E

    val keys: Set<Attr2<Any>>

    operator fun <T : Any> get(key: Attr2<T>): T = tryGet(key) ?: throw QBitException("Entity $this does not contain value for ${key.name}")

    fun <T : Any> tryGet(key: Attr2<T>): T?

    val entries: Set<AttrValue<Attr2<Any>, Any>>
        get() = keys.map {
            it eq this[it]
        }.toSet()

}

fun RoEntity<EID?>.toIdentified(eid: EID): Entity<EID> {
    return DetachedEntity(eid, this)
}

interface Entity<out E : EID?> : RoEntity<E> {

    fun <T : Any> with(key: Attr2<T>, value: T): Entity<E> =
            with(key eq value)

    fun with(vararg values: AttrValue<Attr2<*>, *>): Entity<E>

    fun <T : Any> remove(key: Attr2<T>): Entity<E>

}

interface StoredEntity : Entity<EID> {

    fun peek(key: Attr2<*>) :Any?

    override fun <T : Any> with(key: Attr2<T>, value: T): StoredEntity =
            with(key eq value)

    override fun with(vararg values: AttrValue<Attr2<*>, *>): StoredEntity

    override fun <T : Any> remove(key: Attr2<T>): StoredEntity
}

interface Tombstone : RoEntity<EID>

internal sealed class QRoEntity<out E : EID?>(override val eid: E) : RoEntity<E>

internal class QTombstone(eid: EID) : QRoEntity<EID>(eid), Tombstone {

    override val keys: Set<Attr2<Any>>
        get() = TODO()

    override fun <T : Any> tryGet(key: Attr2<T>): T? =
            when (key) {
                else -> TODO()
            }

}

internal sealed class QEntity<out E : EID?>(eid: E) : QRoEntity<E>(eid), Entity<E>

internal class DetachedEntity<E : EID?>(eid: E, map: Map<Attr2<*>, *>) : QEntity<E>(eid) {

    private val delegate = MapEntity(map) { newMap -> DetachedEntity<E>(eid, newMap) }

    constructor () : this(null as E, emptyMap<Attr2<*>, Any>())

    constructor(eid: EID) : this(eid as E, emptyMap<Attr2<*>, Any>())

    constructor(e: RoEntity<EID?>) : this(e.eid as E, e.entries.map { it.toPair() }.toMap())

    constructor(eid: EID, e: RoEntity<EID?>) : this(eid as E, e.entries.map { it.toPair() }.toMap())

    override val keys: Set<Attr2<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr2<T>): T? {
        return delegate.tryGet(key)
    }

    override fun <T : Any> with(key: Attr2<T>, value: T): DetachedEntity<E> {
        return delegate.with(key, value)
    }

    override fun with(vararg values: AttrValue<Attr2<*>, *>): DetachedEntity<E> {
        return delegate.with(*values)
    }

    override fun <T : Any> remove(key: Attr2<T>): DetachedEntity<E> {
        return delegate.remove(key)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetachedEntity<*>) return false

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

internal class AttachedEntity(eid: EID, map: Map<Attr2<*>, *>, val db: Db, val dirty: Boolean) : QEntity<EID>(eid), StoredEntity {

    private val delegate = MapEntity(map) { newMap -> AttachedEntity(eid, newMap, db, true) }

    override val keys: Set<Attr2<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr2<T>): T? {
        TODO()
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

private class MapEntity<out T : Entity<EID?>>(private val map: Map<Attr2<*>, *>, private val create: (Map<Attr2<*>, *>) -> T) : Entity<EID?> {

    override val eid: EID? = null

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

internal fun RoEntity<EID?>.toFacts(eid: EID): Collection<Fact> =
        this.entries.flatMap { (attr: Attr2<Any>, value) ->
            val type = DataType.of(attr.type)!!
            when  {
                type.value() && !attr.list -> listOf(valToFacts(eid, attr, value))
                type.value() && attr.list -> listToFacts(eid, attr, value as List<Any>)
                type.ref() && !attr.list -> refListToFacts(eid, attr, value as List<Any>)
                type.ref() && attr.list-> listOf(refToFacts(eid, attr, value))
                else -> throw AssertionError("Unexpected attr kind: $attr")
            }
        }

internal fun RoEntity<EID>.toFacts() =
        this.toFacts(eid)

private fun <T : Any> valToFacts(eid: EID, attr: Attr2<T>, value: T) =
        Fact(eid, attr, value)

private fun refToFacts(eid: EID, attr: Attr2<Any>, value: Any) =
        Fact(eid, attr, eidOf(value)!!)

private fun listToFacts(eid: EID, attr: Attr2<*>, value: List<Any>) =
        value.map { Fact(eid, attr, it) }

private fun refListToFacts(eid: EID, attr: Attr2<*>, value: List<Any>) =
        value.map { Fact(eid, attr, eidOf(it)!!) }

private fun eidOf(a: Any): EID? =
        when {
            a is RoEntity<*> && a.eid != null -> a.eid
            a is EID -> a
            else -> null
        }
