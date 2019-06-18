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

data class ScalarAttrValue<T : Any>(override val attr: Attr<T>, override val value: T) : AttrValue<Attr<T>, T>
data class ScalarRefAttrValue(override val attr: ScalarRefAttr, override val value: Entity<*>) : AttrValue<ScalarRefAttr, Entity<*>>
data class ListAttrValue<T : Any>(override val attr: ListAttr<T>, override val value: List<T>) : AttrValue<ListAttr<T>, List<T>>
data class RefListAttrValue(override val attr: RefListAttr, override val value: List<Entity<*>>) : AttrValue<RefListAttr, List<Entity<*>>>

fun Entity(vararg entries: AttrValue<Attr<*>, *>): Entity<EID?> {
    val (refs, values) = entries.partition { it.attr is RefAttr }
    return MapEntity(
            values.map { it.toPair() }.toMap(),
            refs.map {
                when (it.attr) {
                    is ScalarRefAttr -> it.attr to listOf(it.value)
                    is RefListAttr -> it.attr to it.value
                    else -> throw AssertionError("Should never happen")
                }
            }.filterIsInstance<Pair<RefAttr<Any>, List<Entity<*>>>>().toMap(),
            emptyEidResolver, null)
}

internal fun Entity(eid: EID, entries: Collection<Pair<Attr<*>, Any>>, db: Db): StoredEntity = StoredMapEntity(eid,
        entries.filterNot {
            it.first is ScalarRefAttr && it.second is Entity<*> &&
                    it.first is RefListAttr && (it.second as List<*>).all { e -> e is Entity<*> }
        }.toMap(HashMap()),

        (entries.filter { it.first is ScalarRefAttr && it.second is Entity<*> }.filterIsInstance<Pair<ScalarRefAttr, Entity<*>>>() +
                entries.filter { it.first is RefListAttr && (it.second as List<*>).all { e -> e is Entity<*> } }.filterIsInstance<Pair<RefListAttr, List<Entity<*>>>>())
                .toMap(HashMap()) as MutableMap<RefAttr<Any>, List<Entity<*>>>,

        EntityCache(QBitEidResolver(db)),
        dirty = false)

// TODO: make it lazy
internal fun Entity(eid: EID, db: Db): StoredEntity = db.pull(eid)!!

interface Entitiable<out E : EID?> {

    val eid: E

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

    fun detouch() : Entitiable<E>

}

interface Entity<out E : EID?> : Entitiable<E> {

    fun <T : Any> with(key: Attr<T>, value: T): Entity<E>

    fun with(key: ScalarRefAttr, value: Entity<*>): Entity<E>

    fun with(key: RefListAttr, value: List<Entity<*>>): Entity<E>

    fun with(vararg values: AttrValue<Attr<*>, *>): Entity<E>

    fun toIdentified(eid: EID): Entity<EID>

    fun <T : Any> remove(key: Attr<T>): Entity<E>

    override fun detouch(): Entity<E>

}

internal fun <T : Entity<*>> T.setRefs(ref2eid: IdentityHashMap<Entitiable<*>, Entity<EID>>): T =
        this.entries
                .filter { it is ScalarRefAttrValue || (it is RefListAttrValue) }
                .fold(this) { prev, av ->
                    when (av) {
                        is ScalarRefAttrValue -> prev.with(av.attr, ref2eid[prev[av.attr]]!!) as T
                        else -> {
                            val entities: List<Entity<EID>> = (av.value as List<Entity<*>>).map { ref2eid[it]!! }
                            prev.with(av.attr as RefListAttr, entities) as T
                        }
                    }
                }

internal fun Entitiable<*>.toFacts(eid: EID): Collection<Fact> =
        this.entries.flatMap { (attr: Attr<Any>, value) ->
            when (attr) {
                is ScalarRefAttr -> singleton(refToFacts(eid, attr, value))
                is ListAttr<*> -> listToFacts(eid, attr, value as List<Any>)
                is RefListAttr -> refListToFacts(eid, attr, value as List<Any>)
                else -> singleton(attrToFacts(eid, attr, value))
            }
        }

internal fun Entity<EID>.toFacts() =
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
            is Entity<EID?> -> a.eid
            is EID -> a
            else -> null
        }

interface StoredEntity : Entity<EID> {

    val dirty: Boolean

    override fun <T : Any> with(key: Attr<T>, value: T): StoredEntity

    override fun with(key: ScalarRefAttr, value: Entity<*>): StoredEntity

    override fun with(key: RefListAttr, value: List<Entity<*>>): StoredEntity

    override fun with(vararg values: AttrValue<Attr<*>, *>): StoredEntity

}

internal class MapEntity<E : EID?>(
        internal val map: Map<Attr<Any>, Any>,
        internal val refs: Map<RefAttr<Any>, List<Entity<*>>>,
        internal val eidResolver: EidResolver,
        override val eid: E
) :
        Entity<E> {
    init {
        assert { map.keys.intersect(refs.keys).isEmpty() }
    }

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

    override fun <T : Any> with(key: Attr<T>, value: T): MapEntity<E> {
        if (key is ScalarRefAttr) {
            return with(key, value as Entity<*>)
        }
        if (key is RefListAttr) {
            return with(key, value as List<Entity<*>>)
        }

        val newMap: Map<Attr<Any>, Any> = HashMap(map)
        (newMap as MutableMap<Attr<T>, T>)[key] = value

        return MapEntity(newMap, refs, eidResolver, eid)
    }

    override fun with(key: ScalarRefAttr, value: Entity<*>): MapEntity<E> {
        val newRefs = HashMap(refs)
        val newMap = HashMap(map)
        newRefs[key] = singletonList(value)
        newMap.remove(key)
        return MapEntity(newMap, newRefs, eidResolver, eid)
    }

    override fun with(key: RefListAttr, value: List<Entity<*>>): MapEntity<E> {
        val newRefs = HashMap(refs)
        val newMap = HashMap(map)
        newRefs[key] = value
        newMap.remove(key)
        return MapEntity(newMap, newRefs, eidResolver, eid)
    }

    override fun with(vararg values: AttrValue<Attr<*>, *>): MapEntity<E> {
        val newMap = HashMap(map)
        val newRefs = HashMap(refs)
        for (av in values) {
            when (av) {
                is ScalarAttrValue -> newMap[av.attr] = av.value
                is ListAttrValue<*> -> newMap[av.attr] = av.value
                is ScalarRefAttrValue -> {
                    newRefs[av.attr] = listOf(av.value)
                    newMap.remove(av.attr)
                }
                is RefListAttrValue -> {
                    newRefs[av.attr] = av.value
                    newMap.remove(av.attr)
                }
            }
        }

        return MapEntity(newMap, newRefs, eidResolver, eid)
    }

    override val entries: Set<AttrValue<Attr<Any>, Any>>
        get() {
            val scalars = map.entries.map { (attr: Attr<Any>, value) -> ScalarAttrValue(attr, value) }
            val rs = refs.entries.filter { it.key is ScalarRefAttr }.map { (attr: Attr<*>, value) ->
                ScalarRefAttrValue(attr as ScalarRefAttr, value.first())
            }
            val refLists: List<AttrValue<Attr<Any>, Any>> = refs.entries.filter { it.key is RefListAttr }.map { (attr: Attr<*>, value) -> RefListAttrValue(attr as RefListAttr, value) }
            return (scalars + rs + refLists).toSet()
        }

    override fun <T : Any> remove(key: Attr<T>): MapEntity<E> {
        val newRefs = HashMap(refs)
        val newMap = HashMap(map)
        if (key is RefAttr) {
            newRefs.remove(key)
        } else {
            newMap.remove(key)
        }
        return MapEntity(newMap, newRefs, eidResolver, eid)
    }

    override fun toIdentified(eid: EID): Entity<EID> =
            MapEntity(map, refs, eidResolver, eid)

    override fun detouch(): Entity<E> {
        val newMap = HashMap(map)

        for ((key, value) in refs) {
            newMap[key] = value.map { it.eid }
        }
        return MapEntity(newMap, emptyMap(), emptyEidResolver, eid)
    }

}

internal class StoredMapEntity(
        override val eid: EID,
        val map: MutableMap<Attr<Any>, Any>,
        val refs: MutableMap<RefAttr<Any>, List<Entity<*>>>,
        val eidResolver: EidResolver,
        override val dirty: Boolean,
        private val delegate: MapEntity<EID> = MapEntity(map, refs, eidResolver, eid)
) :
        Entity<EID> by delegate,
        StoredEntity {

    constructor(eid: EID, me: MapEntity<*>, dirty: Boolean) : this(eid, HashMap(me.map),
            HashMap(me.refs), me.eidResolver, dirty)

    override fun <T : Any> with(key: Attr<T>, value: T): StoredEntity {
        if ((map as Map<Attr<T>, T>)[key] == value) {
            return this
        }
        return StoredMapEntity(eid, delegate.with(key, value), true)
    }

    override fun with(key: ScalarRefAttr, value: Entity<*>): StoredEntity {
        if (refs[key]?.firstOrNull() == value) {
            return this
        }

        return StoredMapEntity(eid, delegate.with(key, value), true)
    }


    override fun with(key: RefListAttr, value: List<Entity<*>>): StoredEntity {
        if (refs[key] == value) {
            return this
        }

        return StoredMapEntity(eid, delegate.with(key, value), true)
    }

    override fun with(vararg values: AttrValue<Attr<*>, *>): StoredEntity {
        if (values.all { this.getO(it.attr) == it.value }) {
            return this
        }
        return StoredMapEntity(eid, delegate.with(*values), true)
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