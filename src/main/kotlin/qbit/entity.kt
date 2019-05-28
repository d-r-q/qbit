@file:Suppress("UNCHECKED_CAST")

package qbit

import qbit.schema.*
import java.lang.AssertionError
import java.util.*
import java.util.Collections.singleton
import java.util.Collections.singletonList

interface AttrValue<out A : Attr<T>, out T : Any> {

    val attr: A
    val value: T

    fun toPair() = attr to value

    operator fun component1(): Attr<T> = attr

    operator fun component2(): T = value

}

data class ScalarAttrValue<out T : Any>(override val attr: Attr<T>, override val value: T) : AttrValue<Attr<T>, T>
data class ScalarRefAttrValue(override val attr: ScalarRefAttr, override val value: Entity) : AttrValue<ScalarRefAttr, Entity>
data class ListAttrValue<out T : Any>(override val attr: ListAttr<T>, override val value: List<T>) : AttrValue<ListAttr<T>, List<T>>
data class RefListAttrValue(override val attr: RefListAttr, override val value: List<Entity>) : AttrValue<RefListAttr, List<Entity>>

fun Entity(vararg entries: AttrValue<Attr<*>, *>): Entity =
        MapEntity(
                entries.filterNot { it.attr is ScalarRefAttr && it.value is Entity }.map { it.toPair() }.toMap(),
                entries.filter { it.attr is RefAttr && it.value is Entity }.map { it.attr to listOf(it.value) }.filterIsInstance<Pair<RefAttr<Any>, List<Entity>>>().toMap(), emptyEidResolver)

internal fun Entity(eid: EID, entries: Collection<Pair<Attr<*>, Any>>, db: Db): StoredEntity = StoredMapEntity(eid,
        entries.filterNot {
            it.first is ScalarRefAttr && it.second is Entity &&
                    it.first is RefListAttr && (it.second as List<*>).all { e -> e is Entity }
        }.toMap(HashMap()),

        (entries.filter { it.first is ScalarRefAttr && it.second is Entity }.filterIsInstance<Pair<ScalarRefAttr, Entity>>() +
                entries.filter { it.first is RefListAttr && (it.second as List<*>).all { e -> e is Entity } }.filterIsInstance<Pair<RefListAttr, List<Entity>>>())
                .toMap(HashMap()) as MutableMap<RefAttr<Any>, List<Entity>>,

        EntityCache(QBitEidResolver(db)),
        dirty = false)

// TODO: make it lazy
internal fun Entity(eid: EID, db: Db): StoredEntity = db.pull(eid)!!

interface Entitiable {

    val keys: Set<Attr<Any>>

    operator fun <T : Any> get(key: Attr<T>): T = getO(key)!!

    fun <T : Any> getO(key: Attr<T>): T?

    val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = keys.map {
            val value: AttrValue<Attr<Any>, Any> = when (it) {
                is ScalarRefAttr -> ScalarRefAttrValue(it, this[it])
                is ScalarAttr -> ScalarAttrValue(it, this[it])
                else -> throw AssertionError("Should never happen")
            }
            value
        }.toSet()

}

interface Entity : Entitiable {

    fun <T : Any> set(key: Attr<T>, value: T): Entity

    fun set(key: ScalarRefAttr, value: Entity): Entity

    fun set(key: RefListAttr, value: List<Entity>): Entity

    fun set(vararg values: AttrValue<Attr<*>, *>): Entity

    fun toIdentified(eid: EID): IdentifiedEntity
}

internal fun <T : Entity> T.setRefs(ref2eid: IdentityHashMap<Entitiable, IdentifiedEntity>): T =
        this.entries
                .filter { it is ScalarRefAttrValue || (it is RefListAttrValue) }
                .fold(this) { prev, av ->
                    when (av) {
                        is ScalarRefAttrValue -> prev.set(av.attr, ref2eid[prev[av.attr]]!!) as T
                        else -> {
                            val entities: List<IdentifiedEntity> = (av.value as List<Entity>).map { ref2eid[it]!! }
                            prev.set(av.attr as RefListAttr, entities) as T
                        }
                    }
                }

internal fun Entitiable.toFacts(eid: EID): Collection<Fact> =
        this.entries.flatMap { (attr: Attr<Any>, value) ->
            when (attr) {
                is ScalarRefAttr -> singleton(refToFacts(eid, attr, value))
                is ListAttr<*> -> listToFacts(eid, attr, value as List<Any>)
                is RefListAttr -> refListToFacts(eid, attr, value as List<Entity>)
                else -> singleton(attrToFacts(eid, attr, value))
            }
        }


internal fun <T : Any> attrToFacts(eid: EID, attr: Attr<T>, value: T) =
        Fact(eid, attr, value)

internal fun refToFacts(eid: EID, attr: ScalarRefAttr, value: Any) =
        Fact(eid, attr, eidOf(value)!!)

internal fun listToFacts(eid: EID, attr: ListAttr<*>, value: List<Any>) =
        value.map { Fact(eid, attr, it) }

internal fun refListToFacts(eid: EID, attr: RefListAttr, value: List<Entity>) =
        value.map { Fact(eid, attr, eidOf(it)!!) }

internal fun eidOf(a: Any): EID? =
        when (a) {
            is IdentifiedEntity -> a.eid
            is EID -> a
            else -> null
        }

interface IdentifiedEntity : Entity {

    val eid: EID

    override fun <T : Any> set(key: Attr<T>, value: T): IdentifiedEntity

    override fun set(key: ScalarRefAttr, value: Entity): IdentifiedEntity

    override fun set(key: RefListAttr, value: List<Entity>): IdentifiedEntity

    override fun set(vararg values: AttrValue<Attr<*>, *>): IdentifiedEntity

}

interface StoredEntity : IdentifiedEntity {

    val dirty: Boolean

    override fun <T : Any> set(key: Attr<T>, value: T): StoredEntity

    override fun set(key: ScalarRefAttr, value: Entity): StoredEntity

    override fun set(key: RefListAttr, value: List<Entity>): StoredEntity

    override fun set(vararg values: AttrValue<Attr<*>, *>): StoredEntity

}

internal fun IdentifiedEntity.toFacts() =
        this.toFacts(eid)

internal class MapEntity(
        internal val map: Map<Attr<Any>, Any>,
        internal val refs: Map<RefAttr<Any>, List<Entity>>,
        internal val eidResolver: EidResolver
) :
        Entity {
    override val keys: Set<Attr<Any>>
        get() = (map.keys + refs.keys)

    override fun <T : Any> getO(key: Attr<T>): T? {
        return when (key) {
            is ScalarRefAttr -> {
                val res = refs[key]?.firstOrNull() as T?
                val eid: EID? = (map as Map<ScalarRefAttr, EID>)[key]
                (res ?: eid?.let { eidResolver(it) }) as T?
            }
            is RefListAttr -> {
                val res = refs[key] as T?
                val eids: List<EID>? = (map as Map<RefListAttr, List<EID>>)[key]
                (res ?: eids?.let { it.map { item -> eidResolver(item) } }) as T?
            }
            else -> (map as Map<Attr<T>, T>)[key]
        }
    }

    override fun <T : Any> set(key: Attr<T>, value: T): MapEntity {
        val newMap = HashMap(map)
        (newMap as MutableMap<Attr<T>, T>)[key] = value
        return MapEntity(newMap, refs, eidResolver)
    }

    override fun set(key: ScalarRefAttr, value: Entity): MapEntity {
        val newRefs = HashMap(refs)
        newRefs[key] = singletonList(value)
        return MapEntity(map, newRefs, eidResolver)
    }

    override fun set(key: RefListAttr, value: List<Entity>): MapEntity {
        val newRefs = HashMap(refs)
        newRefs[key] = value
        return MapEntity(map, newRefs, eidResolver)
    }

    override fun set(vararg values: AttrValue<Attr<*>, *>): MapEntity {
        val newMap = HashMap(map)
        val newRefs = HashMap(refs)
        for (av in values) {
            when (av) {
                is ScalarAttrValue -> newMap[av.attr] = av.value
                is ListAttrValue<*> -> newMap[av.attr] = av.value
                is ScalarRefAttrValue -> newRefs[av.attr] = listOf(av.value)
                is RefListAttrValue -> newRefs[av.attr] = av.value
            }
        }

        return MapEntity(newMap, newRefs, eidResolver)
    }

    override val entries: Set<AttrValue<Attr<Any>, Any>>
        get() {
            val scalars = map.entries.map { (attr: Attr<Any>, value) -> ScalarAttrValue(attr, value) }
            val rs = refs.entries.filter { it.key is ScalarRefAttr }.map { (attr: Attr<*>, value) -> ScalarRefAttrValue(attr as ScalarRefAttr, value.first()) }
            val refLists: List<AttrValue<Attr<Any>, Any>> = refs.entries.filter { it.key is RefListAttr }.map { (attr: Attr<*>, value) -> RefListAttrValue(attr as RefListAttr, value) }
            return (scalars + rs + refLists).toSet()
        }

    override fun toIdentified(eid: EID): IdentifiedEntity =
            IdentifiedMapEntity(eid, map, refs)
}

private class IdentifiedMapEntity(
        override val eid: EID,
        map: Map<Attr<Any>, Any>,
        refs: Map<RefAttr<Any>, List<Entity>>,
        private val delegate: MapEntity = MapEntity(map, refs, emptyEidResolver)
) :
        Entity by delegate,
        IdentifiedEntity {

    constructor (eid: EID, me: MapEntity) : this(eid, me.map, me.refs)

    override fun <T : Any> set(key: Attr<T>, value: T): IdentifiedEntity {
        return IdentifiedMapEntity(eid, delegate.set(key, value))
    }

    override fun set(key: ScalarRefAttr, value: Entity): IdentifiedEntity {
        return IdentifiedMapEntity(eid, delegate.set(key, value))
    }

    override fun set(key: RefListAttr, value: List<Entity>): IdentifiedEntity {
        return IdentifiedMapEntity(eid, delegate.set(key, value))
    }

    override fun set(vararg values: AttrValue<Attr<*>, *>): IdentifiedEntity {
        return IdentifiedMapEntity(eid, delegate.set(*values))
    }

}

private class StoredMapEntity(
        override val eid: EID,
        val map: MutableMap<Attr<Any>, Any>,
        val refs: MutableMap<RefAttr<Any>, List<Entity>>,
        val eidResolver: EidResolver,
        override val dirty: Boolean,
        private val delegate: MapEntity = MapEntity(map, refs, eidResolver)
) :
        Entity by delegate,
        StoredEntity {

    constructor(eid: EID, me: MapEntity, dirty: Boolean) : this(eid, HashMap(me.map),
            HashMap(me.refs), me.eidResolver, dirty)

    override fun <T : Any> set(key: Attr<T>, value: T): StoredEntity {
        if ((map as Map<Attr<T>, T>)[key] == value) {
            return this
        }
        return StoredMapEntity(eid, delegate.set(key, value), true)
    }

    override fun set(key: ScalarRefAttr, value: Entity): StoredEntity {
        if (refs[key]?.firstOrNull() == value) {
            return this
        }

        return StoredMapEntity(eid, delegate.set(key, value), true)
    }


    override fun set(key: RefListAttr, value: List<Entity>): StoredEntity {
        if (refs[key] == value) {
            return this
        }

        return StoredMapEntity(eid, delegate.set(key, value), true)
    }

    override fun set(vararg values: AttrValue<Attr<*>, *>): StoredEntity {
        if (values.any { this.getO(it.attr) == it.value }) {
            return this
        }
        return StoredMapEntity(eid, delegate.set(*values), true)
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