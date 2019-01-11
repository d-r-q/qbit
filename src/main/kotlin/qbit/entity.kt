@file:Suppress("UNCHECKED_CAST")

package qbit

import qbit.schema.Attr
import qbit.schema.ListAttr
import qbit.schema.RefAttr
import qbit.schema.ScalarAttr
import java.lang.AssertionError
import java.util.*
import java.util.Collections.singleton

interface AttrValue<out A : Attr<T>, T : Any> {

    val attr: A
    val value: T

    fun toPair() = attr to value

    operator fun component1(): Attr<T> = attr

    operator fun component2(): T = value

}

data class ScalarAttrValue<T : Any>(override val attr: Attr<T>, override val value: T) : AttrValue<Attr<T>, T>
data class RefAttrValue(override val attr: RefAttr, override val value: Entity) : AttrValue<RefAttr, Entity>
data class ListAttrValue<T : Any>(override val attr: ListAttr<T>, override val value: List<T>) : AttrValue<ListAttr<T>, List<T>>

fun Entity(vararg entries: AttrValue<Attr<*>, *>): Entity =
        MapEntity(
                entries.filterNot { it.attr is RefAttr && it.value is Entity }.map { it.toPair() }.filterIsInstance<Pair<Attr<Any>, Any>>().toMap(),
                entries.filter { it.attr is RefAttr && it.value is Entity }.map { it.toPair() }.filterIsInstance<Pair<RefAttr, Entity>>().toMap(), emptyEidResolver)

internal fun Entity(eid: EID, entries: Collection<Pair<Attr<*>, Any>>, db: Db): StoredEntity = StoredMapEntity(eid,
        entries.filterNot { it.first is RefAttr && it.second is Entity }.filterIsInstance<Pair<Attr<Any>, Any>>().toMap(HashMap()),
        entries.filter { it.first is RefAttr && it.second is Entity }.filterIsInstance<Pair<RefAttr, Entity>>().toMap(HashMap()),
        EntityCache(QBitEidResolver(db)),
        false, false)

// TODO: make it lazy
internal fun Entity(eid: EID, db: Db): StoredEntity = db.pull(eid)!!

interface Entitiable {

    val keys: Set<Attr<out Any>>

    operator fun <T : Any> get(key: Attr<T>): T?

    val entries: Set<AttrValue<Attr<out Any>, out Any>>
        get() = keys.map {
            val value: AttrValue<Attr<out Any>, out Any> = when (it) {
                is RefAttr -> RefAttrValue(it, this[it]!!)
                is ScalarAttr -> ScalarAttrValue(it as ScalarAttr<Any>, this[it]!!)
                else -> throw AssertionError("Should never happen")
            }
            value
        }.toSet()

}

interface Entity : Entitiable {

    fun <T : Any> set(key: Attr<T>, value: T): Entity

    fun set(key: RefAttr, value: Entity): Entity

    fun toIdentified(eid: EID): IdentifiedEntity
}

internal fun <T : Entity> T.setRefs(ref2eid: IdentityHashMap<Entitiable, IdentifiedEntity>): T =
        this.entries
                .filterIsInstance<RefAttrValue>()
                .fold(this) { prev, av ->
                    prev.set(av.attr, ref2eid[prev[av.attr]!!]!!) as T
                }

internal fun Entitiable.toFacts(eid: EID): Collection<Fact> =
        this.toFacts(eid, false)


internal fun Entitiable.toFacts(eid: EID, deleted: Boolean): Collection<Fact> =
        this.entries.flatMap { (attr: Attr<out Any>, value) ->
            when (attr) {
                is RefAttr -> singleton(refToFacts(eid, attr, value, deleted))
                is ListAttr<*> -> listToFacts(eid, attr, value as List<Any>, deleted)
                else -> singleton(attrToFacts(eid, attr, value, deleted))
            }
        }


internal fun <T : Any> attrToFacts(eid: EID, attr: Attr<out T>, value: T, deleted: Boolean) =
        Fact(eid, attr, value, deleted)

internal fun refToFacts(eid: EID, attr: RefAttr, value: Any, deleted: Boolean) =
        when (value) {
            is IdentifiedEntity -> Fact(eid, attr, value.eid, deleted)
            is EID -> Fact(eid, attr, value, deleted)
            else -> throw AssertionError("Should never happen")
        }

internal fun listToFacts(eid: EID, attr: ListAttr<*>, value: List<Any>, deleted: Boolean) =
        value.map { Fact(eid, attr, it, deleted) }

interface IdentifiedEntity : Entity {

    val eid: EID

    override fun <T : Any> set(key: Attr<T>, value: T): IdentifiedEntity

    override fun set(key: RefAttr, value: Entity): IdentifiedEntity

}

interface StoredEntity : IdentifiedEntity {

    val deleted: Boolean

    val dirty: Boolean

    override fun <T : Any> set(key: Attr<T>, value: T): StoredEntity

    override fun set(key: RefAttr, value: Entity): StoredEntity

    fun delete(): StoredEntity

}

internal fun IdentifiedEntity.toFacts() =
        this.toFacts(eid, (this as? StoredEntity)?.deleted ?: false)

internal class MapEntity(
        internal val map: Map<Attr<Any>, Any>,
        internal val refs: Map<RefAttr, Entity>,
        internal val eidResolver: EidResolver
) :
        Entity {

    override val keys: Set<Attr<out Any>>
        get() = (map.keys + refs.keys)

    override fun <T : Any> get(key: Attr<T>): T? {
        return if (key is RefAttr) {
            val res = refs[key]
            val eid: EID? = (map as Map<RefAttr, EID>)[key]
            (res ?: eid?.let { eidResolver(it) }) as T?
        } else {
            (map as Map<Attr<T>, T>)[key]
        }
    }

    override fun <T : Any> set(key: Attr<T>, value: T): Entity {
        val newMap = HashMap(map)
        (newMap as MutableMap<Attr<T>, T>)[key] = value
        return MapEntity(newMap, refs, eidResolver)
    }

    override fun set(key: RefAttr, value: Entity): Entity {
        val newRefs = HashMap(refs)
        newRefs[key] = value
        return MapEntity(map, newRefs, eidResolver)
    }

    override val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = (map.entries.map { (attr: Attr<Any>, value) -> ScalarAttrValue(attr, value) } +
                refs.entries.map { (attr: Attr<*>, value) -> RefAttrValue(attr, value) }
                ).toSet() as Set<AttrValue<Attr<Any>, Any>>

    override fun toIdentified(eid: EID): IdentifiedEntity =
            IdentifiedMapEntity(eid, map, refs)
}

private class IdentifiedMapEntity(
        override val eid: EID,
        val map: Map<Attr<Any>, Any>,
        val refs: Map<RefAttr, Entity>
) :
        Entity by MapEntity(map, refs, emptyEidResolver),
        IdentifiedEntity {

    override fun <T : Any> set(key: Attr<T>, value: T): IdentifiedEntity {
        val newMap = HashMap(map)
        (newMap as MutableMap<Attr<T>, T>)[key] = value
        return IdentifiedMapEntity(eid, newMap, refs)
    }

    override fun set(key: RefAttr, value: Entity): IdentifiedEntity {
        val newRefs = HashMap(refs)
        newRefs[key] = value
        return IdentifiedMapEntity(eid, map, newRefs)
    }
}

private class StoredMapEntity(
        override val eid: EID,
        val map: MutableMap<Attr<Any>, Any>,
        val refs: MutableMap<RefAttr, Entity>,
        val eidResolver: EidResolver,
        override val deleted: Boolean,
        override val dirty: Boolean
) :
        Entity by MapEntity(map, refs, eidResolver),
        StoredEntity {

    override fun delete(): StoredEntity =
            StoredMapEntity(eid, map, refs, eidResolver, true, true)

    override fun <T : Any> set(key: Attr<T>, value: T): StoredEntity {
        if (deleted) {
            throw QBitException("Could not change entity marked for deletion")
        }
        if ((map as Map<Attr<T>, T>)[key] == value) {
            return this
        }
        val newMap = HashMap<Attr<Any>, Any>(map)
        (newMap as MutableMap<Attr<T>, T>)[key] = value
        return StoredMapEntity(eid, newMap, refs, eidResolver, deleted, true)
    }

    override fun set(key: RefAttr, value: Entity): StoredEntity {
        if (deleted) {
            throw QBitException("Could not change entity marked for deletion")
        }
        if (refs[key] == value) {
            return this
        }

        val newRefs = HashMap(refs)
        newRefs[key] = value
        return StoredMapEntity(eid, map, newRefs, eidResolver, deleted, true)
    }
}

typealias EidResolver = (EID) -> StoredEntity?

private val emptyEidResolver: EidResolver = { null }

private class QBitEidResolver(private val qbit: Db) : EidResolver {

    override fun invoke(eid: EID): StoredEntity? =
            qbit.pull(eid)

}

private class EntityCache(private val delegate: EidResolver) : EidResolver {

    private val cache = HashMap<EID, StoredEntity?>()

    override fun invoke(key: EID): StoredEntity? =
            cache.getOrPut(key) { delegate(key) }

}