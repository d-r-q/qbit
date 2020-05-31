package qbit

import qbit.api.QBitException
import qbit.api.db.Fetch
import qbit.api.db.QueryPred
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.*
import qbit.api.model.impl.DetachedEntity
import qbit.api.model.impl.QStoredEntity
import qbit.index.Index
import qbit.index.IndexDb
import qbit.index.InternalDb
import qbit.test.model.testsSerialModule
import kotlin.reflect.KClass
import kotlin.test.assertEquals

internal fun dbOf(eids: Iterator<Gid> = Gid(0, 0).nextGids(), vararg entities: Any): InternalDb {
    val addedAttrs = entities
            .filterIsInstance<Attr<*>>()
            .map { it.name to it }
            .toMap()
    val facts = entities.flatMap { testSchemaFactorizer.factor(it, (bootstrapSchema + addedAttrs)::get, eids) }
    return IndexDb(Index(facts.groupBy { it.gid }.map { it.key to it.value }), testsSerialModule)
}

internal object EmptyDb : InternalDb() {

    override fun pullEntity(gid: Gid): StoredEntity? = null

    override fun <R : Any> pull(gid: Gid, type: KClass<R>, fetch: Fetch): R? = null

    override fun queryGids(vararg preds: QueryPred): Sequence<Gid> = emptySequence()

    override fun query(vararg preds: QueryPred): Sequence<Entity> = emptySequence()

    override fun attr(attr: String): Attr<Any>? = bootstrapSchema[attr]

    override fun with(facts: Iterable<Eav>): InternalDb {
        return IndexDb(Index().addFacts(facts), testsSerialModule)
    }

}

inline fun <reified T : Any> Attr(name: String, unique: Boolean = true): Attr<T> =
        Attr(null, name, unique)

inline fun <reified T : Any, reified L : List<T>> ListAttr(id: Gid?, name: String, unique: Boolean = true): Attr<L> {
    return Attr(
        id,
        name,
        types[T::class]?.code ?: throw QBitException("Unsupported type: ${T::class} for attribute: $name"),
        unique,
        false
    )
}

inline fun <reified T : Any> Attr(id: Gid?, name: String, unique: Boolean = true): Attr<T> {
    return Attr(
        id,
        name,
        types[T::class]?.code ?: throw QBitException("Unsupported type: ${T::class} for attribute: $name"),
        unique,
        false
    )
}

val types: Map<KClass<*>, DataType<*>> = mapOf(
    Boolean::class to QBoolean,
    Byte::class to QByte,
    Int::class to QInt,
    Long::class to QLong,
    String::class to QString,
    ByteArray::class to QBytes,
    Gid::class to QGid,
    Any::class to QRef
)


fun Entity(gid: Gid, vararg entries: Any): Entity {
    return DetachedEntity(gid, entries.filterIsInstance<AttrValue<Attr<Any>, Any>>().map { it.attr to entity2gid(it.value) }.toMap())
}

fun AttachedEntity(gid: Gid, entries: Map<Attr<Any>, Any>, resolveGid: (Gid) -> StoredEntity?): StoredEntity {
    return QStoredEntity(gid, entries.toMap().mapValues { if (it.value is Entity) (it.value as Entity).gid else it.value }, resolveGid)
}

fun assertArrayEquals(arr1: Array<*>?, arr2: Array<*>?) {
    arr1!!; arr2!!
    assertEquals(arr1.size, arr2.size)
    (arr1 zip arr2).forEach { assertEquals(it.first, it.second) }
}

fun assertArrayEquals(arr1: ByteArray?, arr2: ByteArray?) {
    arr1!!; arr2!!
    assertEquals(arr1.size, arr2.size)
    (arr1 zip arr2).forEach { assertEquals(it.first, it.second) }
}

