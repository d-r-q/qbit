@file:Suppress("UNCHECKED_CAST")

package qbit.model

import qbit.*
import qbit.platform.IdentityHashMap
import qbit.platform.set

interface AttrValue<out A : Attr<T>, out T : Any> {

    val attr: A
    val value: T

    fun toPair() = attr to value

    operator fun component1(): Attr<T> = attr

    operator fun component2(): T = value

}

data class ScalarAttrValue<T : Any>(override val attr: Attr<T>, override val value: T) : AttrValue<Attr<T>, T>
data class ScalarRefAttrValue(override val attr: ScalarRefAttr, override val value: RoEntity<*>) : AttrValue<ScalarRefAttr, RoEntity<*>>
data class ListAttrValue<T : Any>(override val attr: ListAttr<T>, override val value: List<T>) : AttrValue<ListAttr<T>, List<T>>
data class RefListAttrValue(override val attr: RefListAttr, override val value: List<RoEntity<*>>) : AttrValue<RefListAttr, List<RoEntity<*>>>

fun Entity(vararg entries: AttrValue<Attr<*>, *>): Entity<EID?> {
    return DetachedEntity(null, entries.map { it.toPair() }.toMap())
}

fun Tombstone(eid: EID): Tombstone = QTombstone(eid)

internal fun Entity(eid: EID, entries: Collection<Pair<Attr<*>, Any>>, db: Db): StoredEntity = AttachedEntity(eid, entries.toMap(), db, false)

// TODO: make it lazy
internal fun Entity(eid: EID, db: Db): StoredEntity = db.pull(eid)!!


interface RoEntity<out E : EID?> {

    val eid: E

    val keys: Set<Attr<Any>>

    operator fun <T : Any> get(key: Attr<T>): T = tryGet(key) ?: throw QBitException("Entity $this does not contain value for ${key.str()}")

    fun <T : Any> tryGet(key: Attr<T>): T?

    val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = keys.map {
            it eq this[it]
        }.toSet()

}

fun RoEntity<EID?>.toIdentified(eid: EID): Entity<EID> {
    return DetachedEntity(eid, this)
}

fun RoEntity<EID>.tombstone(): Tombstone {
    return Tombstone(eid)
}

interface Entity<out E : EID?> : RoEntity<E> {

    fun <T : Any> with(key: Attr<T>, value: T): Entity<E> =
            with(key eq value)

    fun with(vararg values: AttrValue<Attr<*>, *>): Entity<E>

    fun <T : Any> remove(key: Attr<T>): Entity<E>

}

interface StoredEntity : Entity<EID> {

    fun peek(key: Attr<*>) :Any?

    override fun <T : Any> with(key: Attr<T>, value: T): StoredEntity =
            with(key eq value)

    override fun with(vararg values: AttrValue<Attr<*>, *>): StoredEntity

    override fun <T : Any> remove(key: Attr<T>): StoredEntity
}

interface Tombstone : RoEntity<EID>

internal sealed class QRoEntity<out E : EID?>(override val eid: E) : RoEntity<E>

internal class QTombstone(eid: EID) : QRoEntity<EID>(eid), Tombstone {

    override val keys: Set<Attr<Any>>
        get() = setOf(tombstone)

    override fun <T : Any> tryGet(key: Attr<T>): T? =
            when (key) {
                tombstone -> true as T
                else -> null
            }

}

internal sealed class QEntity<out E : EID?>(eid: E) : QRoEntity<E>(eid), Entity<E>

internal class DetachedEntity<E : EID?>(eid: E, map: Map<Attr<*>, *>) : QEntity<E>(eid) {

    private val delegate = MapEntity(map) { newMap -> DetachedEntity<E>(eid, newMap) }

    constructor () : this(null as E, emptyMap<Attr<*>, Any>())

    constructor(eid: EID) : this(eid as E, emptyMap<Attr<*>, Any>())

    constructor(e: RoEntity<EID?>) : this(e.eid as E, e.entries.map { it.toPair() }.toMap())

    constructor(eid: EID, e: RoEntity<EID?>) : this(eid as E, e.entries.map { it.toPair() }.toMap())

    override val keys: Set<Attr<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        return delegate.tryGet(key)
    }

    override fun <T : Any> with(key: Attr<T>, value: T): DetachedEntity<E> {
        return delegate.with(key, value)
    }

    override fun with(vararg values: AttrValue<Attr<*>, *>): DetachedEntity<E> {
        return delegate.with(*values)
    }

    override fun <T : Any> remove(key: Attr<T>): DetachedEntity<E> {
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

internal class AttachedEntity(eid: EID, map: Map<Attr<*>, *>, val db: Db, val dirty: Boolean) : QEntity<EID>(eid), StoredEntity {

    private val delegate = MapEntity(map) { newMap -> AttachedEntity(eid, newMap, db, true) }

    override val keys: Set<Attr<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        val value = delegate.tryGet(key)
        return if (key is RefAttr && value is EID) {
            db.pull(value) as T?
        } else if (key is RefListAttr && value is List<*> && value.size > 0 && value[0] is EID) {
            value.map { db.pull(it as EID) } as T?
        } else {
            value
        }
    }

    override fun  peek(key: Attr<*>): Any? =
            delegate.tryGet(key)

    override fun <T : Any> with(key: Attr<T>, value: T): AttachedEntity {
        if (value == this.tryGet(key)) {
            return this
        }
        return delegate.with(key, value)
    }

    override fun with(vararg values: AttrValue<Attr<*>, *>): AttachedEntity {
        return delegate.with(*values)
    }

    override fun <T : Any> remove(key: Attr<T>): AttachedEntity {
        return delegate.remove(key)
    }

}

private class MapEntity<out T : Entity<EID?>>(private val map: Map<Attr<*>, *>, private val create: (Map<Attr<*>, *>) -> T) : Entity<EID?> {

    override val eid: EID? = null

    override val keys: Set<Attr<Any>>
        get() = map.keys

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        return map[key] as T?
    }

    override fun <V : Any> with(key: Attr<V>, value: V): T =
            with(key eq value)

    override fun with(vararg values: AttrValue<Attr<*>, *>): T {
        val newMap = HashMap(map)
        for ((key, value) in values) {
            newMap[key] = value
        }
        return create(newMap)
    }

    override fun <V : Any> remove(key: Attr<V>): T {
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

internal fun unfoldEntitiesGraph(es: Collection<RoEntity<*>>, eids: Iterator<EID>): IdentityHashMap<RoEntity<*>, RoEntity<EID>> {
    val res = IdentityHashMap<RoEntity<*>, RoEntity<EID>>()

    fun body(es: Collection<RoEntity<*>>) {
        es.forEach {
            if (!res.containsKey(it)) {
                @Suppress("UNCHECKED_CAST")
                when  {
                    it.eid != null -> res[it] = it as RoEntity<EID>
                    else -> res[it] = it.toIdentified(eids.next())
                }
            }
            it.keys.forEach { attr ->
                if (attr is ScalarRefAttr) {
                    val value: RoEntity<*> = it[attr]
                    if (res[value] == null) {
                        body(listOf(value))
                    }
                } else if (attr is RefListAttr) {
                    body(it[attr])
                }
            }
        }
    }
    body(es)

    return res
}

internal fun <T : RoEntity<EID>> T.setRefs(ref2eid: IdentityHashMap<RoEntity<*>, RoEntity<EID>>): DetachedEntity<EID> =
        this.entries
                .filter { it is ScalarRefAttrValue || (it is RefListAttrValue) }
                .fold(DetachedEntity(this)) { prev, av ->
                    when (av) {
                        is ScalarRefAttrValue -> {
                            prev.with(av.attr eq ref2eid[prev[av.attr]]!!)
                        }
                        else -> {
                            val entities: List<RoEntity<*>> = (av.value as List<RoEntity<*>>).map { ref2eid[it]!! }
                            prev.with(av.attr as RefListAttr, entities)
                        }
                    }
                }

internal fun RoEntity<EID?>.toFacts(eid: EID): Collection<Fact> =
        this.entries.flatMap { (attr: Attr<Any>, value) ->
            when (attr) {
                is ScalarRefAttr -> listOf(refToFacts(eid, attr, value))
                is ListAttr<*> -> listToFacts(eid, attr, value as List<Any>)
                is RefListAttr -> refListToFacts(eid, attr, value as List<Any>)
                is ScalarAttr -> listOf(attrToFacts(eid, attr, value))
            }
        }

internal fun RoEntity<EID>.toFacts() =
        this.toFacts(eid)

private fun <T : Any> attrToFacts(eid: EID, attr: Attr<T>, value: T) =
        Fact(eid, attr, value)

private fun refToFacts(eid: EID, attr: ScalarRefAttr, value: Any) =
        Fact(eid, attr, eidOf(value)!!)

private fun listToFacts(eid: EID, attr: ListAttr<*>, value: List<Any>) =
        value.map { Fact(eid, attr, it) }

private fun refListToFacts(eid: EID, attr: RefListAttr, value: List<Any>) =
        value.map { Fact(eid, attr, eidOf(it)!!) }

private fun eidOf(a: Any): EID? =
        when {
            a is RoEntity<*> && a.eid != null -> a.eid
            a is EID -> a
            else -> null
        }
