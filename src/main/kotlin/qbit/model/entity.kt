@file:Suppress("UNCHECKED_CAST")

package qbit.model

import qbit.Db
import qbit.Fact
import qbit.tombstone
import java.util.*
import java.util.Collections.singleton

interface AttrValue<out A : Attr<T>, out T : Any> {

    val attr: A
    val value: T

    fun toPair() = attr to value

    operator fun component1(): Attr<T> = attr

    operator fun component2(): T = value

}

data class ScalarAttrValue<T : Any>(override val attr: Attr<T>, override val value: T) : AttrValue<Attr<T>, T>
data class ScalarRefAttrValue(override val attr: ScalarRefAttr, override val value: Entitiable) : AttrValue<ScalarRefAttr, Entitiable>
data class ListAttrValue<T : Any>(override val attr: ListAttr<T>, override val value: List<T>) : AttrValue<ListAttr<T>, List<T>>
data class RefListAttrValue(override val attr: RefListAttr, override val value: List<Entitiable>) : AttrValue<RefListAttr, List<Entitiable>>

fun Entity(vararg entries: AttrValue<Attr<*>, *>): MutableEntitiable {
    return ProtoEntity(entries.map { it.toPair() }.toMap())
}

internal fun Entity(eid: EID, entries: Collection<Pair<Attr<*>, Any>>, db: Db): AttachedEntity = AttachedEntity(eid, entries.toMap(), db, false)

// TODO: make it lazy
internal fun Entity(eid: EID, db: Db): AttachedEntity = db.pull(eid)!!

interface Entitiable {

    val keys: Set<Attr<Any>>

    operator fun <T : Any> get(key: Attr<T>): T = tryGet(key)!!

    fun <T : Any> tryGet(key: Attr<T>): T?

    val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = keys.map {
            it eq this[it]
        }.toSet()

}

interface MutableEntitiable : Entitiable {

    fun <T : Any> with(key: Attr<T>, value: T): MutableEntitiable =
            with(key eq value)

    //fun with(key: ScalarRefAttr, value: Entitiable): Entitiable

    // fun with(key: RefListAttr, value: List<Entitiable>): Entitiable

    fun with(vararg values: AttrValue<Attr<*>, *>): MutableEntitiable

    fun <T : Any> remove(key: Attr<T>): MutableEntitiable

}

interface MaybeEntity {

    val eid: EID?

}

fun Entitiable.toIdentified(eid: EID): MutableEntity {
    return DetachedEntity(eid, this)
}

class ProtoEntity(internal val map: Map<Attr<*>, *>) : MutableEntitiable {

    constructor() : this(emptyMap<Attr<Any>, Any>())

    override val keys: Set<Attr<Any>>
        get() = map.keys

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        return map[key] as T?
    }

    override fun with(vararg values: AttrValue<Attr<*>, *>): MutableEntitiable {
        val newMap = HashMap(map)
        for ((key, value) in values) {
            newMap[key] = value
        }
        return ProtoEntity(newMap)
    }

    override fun <T : Any> remove(key: Attr<T>): MutableEntitiable {
        val newMap = HashMap(map)
        newMap -= key
        return ProtoEntity(newMap)
    }

}

sealed class Entity(val eid: EID) : Entitiable

sealed class MutableEntity(eid: EID) : Entity(eid), MutableEntitiable

class DetachedEntity(eid: EID, map: Map<Attr<*>, *>) : MutableEntity(eid) {

    private val delegate = Map2Entity(map) { newMap -> DetachedEntity(eid, newMap) }

    constructor(eid: EID) : this(eid, emptyMap<Attr<*>, Any>())

    constructor(e: Entity) : this(e.eid, e.entries.map { it.toPair() }.toMap())

    constructor(eid: EID, e: Entitiable) : this(eid, e.entries.map { it.toPair() }.toMap())

    override val keys: Set<Attr<Any>>
        get() = delegate.keys

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        return delegate.tryGet(key)
    }

    override fun <T : Any> with(key: Attr<T>, value: T): DetachedEntity {
        return delegate.with(key, value)
    }

    override fun with(vararg values: AttrValue<Attr<*>, *>): DetachedEntity {
        return delegate.with(*values)
    }

    override fun <T : Any> remove(key: Attr<T>): DetachedEntity {
        return delegate.remove(key)
    }
}

private class Map2Entity<T : MutableEntitiable>(private val map: Map<Attr<*>, *>, private val create: (Map<Attr<*>, *>) -> T) : MutableEntitiable {

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
}

internal fun <T : Entity> T.setRefs(ref2eid: IdentityHashMap<Entitiable, Entity>): DetachedEntity =
        this.entries
                .filter { it is ScalarRefAttrValue || (it is RefListAttrValue) }
                .fold(DetachedEntity(this)) { prev, av ->
                    when (av) {
                        is ScalarRefAttrValue -> prev.with(av.attr, ref2eid[prev[av.attr]]!!)
                        else -> {
                            val entities: List<Entity> = (av.value as List<Entitiable>).map { ref2eid[it]!! }
                            prev.with(av.attr as RefListAttr, entities)
                        }
                    }
                }

internal fun Entitiable.toFacts(eid: EID): Collection<Fact> =
        this.entries.flatMap { (attr: Attr<Any>, value) ->
            when (attr) {
                is ScalarRefAttr -> singleton(refToFacts(eid, attr, value))
                is ListAttr<*> -> listToFacts(eid, attr, value as List<Any>)
                is RefListAttr -> refListToFacts(eid, attr, value as List<Any>)
                else -> singleton(attrToFacts(eid, attr, value))
            }
        }

internal fun Entity.toFacts() =
        this.toFacts(eid)

internal fun <T : Any> attrToFacts(eid: EID, attr: Attr<T>, value: T) =
        Fact(eid, attr, value)

internal fun refToFacts(eid: EID, attr: ScalarRefAttr, value: Any) =
        Fact(eid, attr, eidOf(value)!!)

internal fun listToFacts(eid: EID, attr: ListAttr<*>, value: List<Any>) =
        value.map { Fact(eid, attr, it) }

internal fun refListToFacts(eid: EID, attr: RefListAttr, value: List<Any>) =
        value.map { Fact(eid, attr, eidOf(it)!!) }

internal fun eidOf(a: Any): EID? =
        when (a) {
            is Entity -> a.eid
            is EID -> a
            else -> null
        }

class Tombstone(eid: EID) : Entity(eid) {
    override val keys: Set<Attr<Any>>
        get() = setOf(tombstone)

    override fun <T : Any> tryGet(key: Attr<T>): T? =
            when (key) {
                tombstone -> true as T
                else -> null
            }

}

class AttachedEntity(eid: EID, map: Map<Attr<*>, *>, val db: Db, val dirty: Boolean) : MutableEntity(eid) {

    private val delegate = Map2Entity(map) { newMap -> AttachedEntity(eid, newMap, db, true) }

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

