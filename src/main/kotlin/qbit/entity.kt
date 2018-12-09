package qbit

import qbit.schema.Attr
import qbit.schema.RefAttr
import kotlin.reflect.KClass

fun Entity(vararg entries: Pair<Attr<*>, Any>): Entity =
        MapEntity(
                entries.filterNot { it.first is RefAttr && it.second is Entity }.toMap(),
                entries.filter { it.first is RefAttr && it.second is Entity }.filterIsInstance<Pair<RefAttr, Entity>>().toMap(), emptyEidResolver)

internal fun Entity(eid: EID, entries: Collection<Pair<Attr<*>, Any>>, db: Db): StoredEntity = StoredMapEntity(eid,
        entries.filterNot { it.first is RefAttr && it.second is Entity }.toMap(HashMap()),
        entries.filter { it.first is RefAttr && it.second is Entity }.filterIsInstance<Pair<RefAttr, Entity>>().toMap(HashMap()),
        EntityCache(QBitEidResolver(db)),
        false, false)

// TODO: make it lazy
internal fun Entity(eid: EID, db: Db): StoredEntity = db.pull(eid)!!

interface Entity {

    val keys: Set<Attr<*>>

    operator fun <T : Any> get(key: Attr<T>): T?

    operator fun get(key: RefAttr): Entity?

    fun <T : Any> set(key: Attr<T>, value: T): Entity

    fun set(key: RefAttr, value: Entity): Entity

    val entries: Set<Map.Entry<Attr<*>, Any>>
        get() = keys.map {
            object : Map.Entry<Attr<*>, Any> {
                override val key = it
                override val value: Any = this@Entity[it as Attr<Any>]!!
            }
        }.toSet()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(keyStr: Attr<T>, type: KClass<T>): T? = get(keyStr)?.let {
        if (type == Byte::class && it is Byte) {
            it
        } else if (type == Boolean::class && it is Boolean) {
            it
        } else if (type.javaObjectType.isAssignableFrom(it.javaClass)) {
            type.java.cast(it)
        } else {
            throw IllegalArgumentException("Could not cast ${it.javaClass} to $type")
        }
    }

    fun toIdentified(eid: EID): IdentifiedEntity
}

internal fun Entity.toFacts(eid: EID): Collection<Fact> =
        this.toFacts(eid, false)


internal fun Entity.toFacts(eid: EID, deleted: Boolean): Collection<Fact> =
        this.entries.map { (attr: Attr<*>, value) ->
            when (attr) {
                is RefAttr -> refToFacts(eid, attr, value as IdentifiedEntity, deleted)
                else -> attrToFacts(eid, attr as Attr<Any>, value, deleted)
            }
        }


internal fun <T : Any> attrToFacts(eid: EID, attr: Attr<T>, value: T, deleted: Boolean) =
        Fact(eid, attr, value, deleted)

internal fun refToFacts(eid: EID, attr: RefAttr, value: IdentifiedEntity, deleted: Boolean) =
        Fact(eid, attr, value.eid, deleted)

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
        internal val map: Map<Attr<*>, Any>,
        internal val refs: Map<RefAttr, Entity>,
        internal val eidResolver: EidResolver
) :
        Entity {

    override val keys: Set<Attr<*>>
        get() = map.keys + refs.keys

    override fun <T : Any> get(key: Attr<T>): T? =
            map[key] as T?

    override fun get(key: RefAttr): Entity? {
        val res = refs[key]
        val eid = map[key] as EID?
        return res ?: eid?.let { eidResolver(it) }
    }

    override fun <T : Any> set(key: Attr<T>, value: T): Entity {
        val newMap = HashMap(map)
        newMap[key] = value
        return MapEntity(newMap, refs, eidResolver)
    }

    override fun set(key: RefAttr, value: Entity): Entity {
        val newRefs = HashMap(refs)
        newRefs[key] = value
        return MapEntity(map, newRefs, eidResolver)
    }

    override val entries: Set<Map.Entry<Attr<*>, Any>>
        get() = map.entries + refs.entries

    override fun toIdentified(eid: EID): IdentifiedEntity =
            IdentifiedMapEntity(eid, map, refs)
}

private class IdentifiedMapEntity(
        override val eid: EID,
        val map: Map<Attr<*>, Any>,
        val refs: Map<RefAttr, Entity>
) :
        Entity by MapEntity(map, refs, emptyEidResolver),
        IdentifiedEntity {

    override fun <T : Any> set(key: Attr<T>, value: T): IdentifiedEntity {
        val newMap = HashMap(map)
        newMap[key] = value
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
        val map: MutableMap<Attr<*>, Any>,
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
        if (map[key] == value) {
            return this
        }
        val newMap = HashMap(map)
        newMap[key] = value
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

private val emptyEidResolver: EidResolver = { eid -> null }

private class QBitEidResolver(private val qbit: Db) : EidResolver {

    override fun invoke(eid: EID): StoredEntity? =
            qbit.pull(eid)

}

private class EntityCache(private val delegate: EidResolver) : EidResolver {

    private val cache = HashMap<EID, StoredEntity?>()

    override fun invoke(key: EID): StoredEntity? =
            cache.getOrPut(key) { delegate(key) }

}