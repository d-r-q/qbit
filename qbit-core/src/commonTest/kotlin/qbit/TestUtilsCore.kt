package qbit

import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.plus
import qbit.api.QBitException
import qbit.api.db.Conn
import qbit.api.db.Fetch
import qbit.api.db.QueryPred
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.*
import qbit.api.model.impl.DetachedEntity
import qbit.api.model.impl.QStoredEntity
import qbit.factoring.serializatoin.KSFactorizer
import qbit.index.Index
import qbit.index.IndexDb
import qbit.index.Indexer
import qbit.index.InternalDb
import qbit.serialization.Node
import qbit.serialization.NodeVal
import qbit.spi.Storage
import qbit.storage.MemStorage
import qbit.test.model.testsSerialModule
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.fail

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

val testSchemaFactorizer =
    KSFactorizer(qbitSerialModule + testsSerialModule)

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

val identityNodeResolver: (Node<Hash>) -> NodeVal<Hash>? = { it as? NodeVal<Hash> }

fun mapNodeResolver(map: Map<Hash, NodeVal<Hash>>): (Node<Hash>) -> NodeVal<Hash>? = { n -> map[n.parentHash] }

internal fun TestIndexer(
    serialModule: SerialModule = testsSerialModule,
    baseDb: IndexDb? = null,
    baseHash: Hash? = null,
    nodeResolver: (Node<Hash>) -> NodeVal<Hash>? = identityNodeResolver) =
    Indexer(serialModule, baseDb, baseHash, nodeResolver)

inline fun <reified E : Throwable> assertThrows(body: () -> Unit) {
    try {
        body()
        fail("${E::class} exception expected")
    } catch (e : Throwable) {
        if (e !is E) {
            throw e
        }
    }
}

inline fun <reified T : Any> Attr(name: String, unique: Boolean = true): Attr<T> =
    Attr(null, name, unique)

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

internal fun dbOf(eids: Iterator<Gid> = Gid(0, 0).nextGids(), vararg entities: Any): InternalDb {
    val addedAttrs = entities
        .filterIsInstance<Attr<*>>()
        .map { it.name to it }
        .toMap()
    val facts = entities.flatMap { testSchemaFactorizer.factor(it, (bootstrapSchema + addedAttrs)::get, eids) }
    return IndexDb(Index(facts.groupBy { it.gid }.map { it.key to it.value }), testsSerialModule)
}

inline fun <reified T : Any, reified L : List<T>> ListAttr(id: Gid?, name: String, unique: Boolean = true): Attr<L> {
    return Attr(
        id,
        name,
        types[T::class]?.code ?: throw QBitException("Unsupported type: ${T::class} for attribute: $name"),
        unique,
        false
    )
}

suspend fun setupTestSchema(storage: Storage = MemStorage()): Conn {
    val conn = qbit(storage, testsSerialModule)
    testSchema.forEach {
        conn.persist(it)
    }
    return conn
}

suspend fun setupTestData(storage: Storage = MemStorage()): Conn {
    return with(setupTestSchema(storage)) {
        listOf(eCodd, pChen, mStonebreaker, eBrewer, uk, tw, us, ru, nsk).forEach {
            persist(it)
        }
        this
    }
}
