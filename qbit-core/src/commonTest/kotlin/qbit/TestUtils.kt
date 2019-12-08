package qbit

import qbit.api.*
import qbit.api.db.Fetch
import qbit.api.db.QueryPred
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.*
import qbit.factorization.destruct
import qbit.factorization.types
import qbit.index.Index
import qbit.index.IndexDb
import qbit.index.InternalDb
import qbit.model.Entity
import qbit.model.StoredEntity
import qbit.model.entity2gid
import qbit.model.impl.DetachedEntity
import qbit.model.impl.QStoredEntity
import qbit.platform.*
import qbit.serialization.Node
import qbit.serialization.NodeVal
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.fail

internal fun dbOf(eids: Iterator<Gid> = Gid(0, 0).nextGids(), vararg entities: Any): InternalDb {
    val addedAttrs = entities
            .filterIsInstance<Attr<*>>()
            .map { it.name to it }
            .toMap()
    val facts = entities.flatMap { destruct(it, (bootstrapSchema + addedAttrs)::get, eids) }
    return IndexDb(Index(facts.groupBy { it.gid }.map { it.key to it.value }))
}

internal object EmptyDb : InternalDb() {

    override fun pullEntity(gid: Gid): StoredEntity? = null

    override fun <R : Any> pull(gid: Gid, type: KClass<R>, fetch: Fetch): R? = null

    override fun queryGids(vararg preds: QueryPred): Sequence<Gid> = emptySequence()

    override fun query(vararg preds: QueryPred): Sequence<Entity> = emptySequence()

    override fun attr(attr: String): Attr<Any>? = bootstrapSchema[attr]

    override fun with(facts: Iterable<Eav>): InternalDb {
        return IndexDb(Index().addFacts(facts))
    }

}

internal class EntityMapDb(private val map: Map<Gid, StoredEntity>) : InternalDb() {

    override fun pullEntity(gid: Gid): StoredEntity? {
        return map[gid]
    }

    override fun <R : Any> pull(gid: Gid, type: KClass<R>, fetch: Fetch): R? {
        TODO("not implemented")
    }

    override fun query(vararg preds: QueryPred): Sequence<Entity> {
        TODO("not implemented")
    }

    override fun attr(attr: String): Attr<Any>? {
        TODO("not implemented")
    }

    override fun with(facts: Iterable<Eav>): InternalDb {
        TODO("not implemented")
    }

    override fun queryGids(vararg preds: QueryPred): Sequence<Gid> {
        TODO("not implemented")
    }

}

val nullNodeResolver: (Node<Hash>) -> NodeVal<Hash>? = { null }

val identityNodeResolver: (Node<Hash>) -> NodeVal<Hash>? = { it as? NodeVal<Hash> }

val nullGidResolver: (Gid) -> StoredEntity? = { null }

fun mapNodeResolver(map: Map<Hash, NodeVal<Hash>>): (Node<Hash>) -> NodeVal<Hash>? = { n -> map[n.hash] }

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

fun createBombWithoutNulls(): Bomb {
    val country = Country(0, "Country", 0)
    val bomb = Bomb(
            null,

            true,
            false,
            listOf(true, false, true),
            emptyList(),
            false,
            false,
            listOf(false, true, false),
            listOf(true),

            0,
            1,
            listOf(-128, -1, 0, 1, 127),
            emptyList(),

            0,
            1,
            listOf(Int.MIN_VALUE, 0, Int.MAX_VALUE),
            listOf(2),

            0,
            -1024,
            listOf(Long.MIN_VALUE, 0, Long.MAX_VALUE),
            listOf(1024),

            Instants.now(),
            Instants.now(),
            listOf(Instants.ofEpochMilli(Long.MIN_VALUE), Instants.ofEpochMilli(0), Instants.ofEpochMilli(Long.MAX_VALUE)),
            emptyList(),


            BigDecimal(0),
            BigDecimal(Long.MAX_VALUE).plus(BigDecimal(Long.MAX_VALUE)),
            listOf(BigDecimal(Long.MAX_VALUE).plus(BigDecimal(1)), BigDecimal(Long.MIN_VALUE),
                    BigDecimal(Long.MAX_VALUE), BigDecimal(Long.MAX_VALUE).plus(BigDecimal(1))),
            listOf(BigDecimal(Long.MIN_VALUE).minus(BigDecimal(Long.MIN_VALUE))),

            ZonedDateTimes.ofInstant(Instants.ofEpochMilli(0), ZoneIds.of("UTC")),
            ZonedDateTimes.now(),
            listOf(ZonedDateTimes.now(),
                    ZonedDateTimes.ofInstant(Instants.ofEpochMilli(Long.MAX_VALUE), ZoneIds.of("UTC")),
                    ZonedDateTimes.of(-2200, 1, 1, 0, 0, 0, 0, ZoneIds.of("+01:00")),
                    ZonedDateTimes.now(ZoneIds.of("Europe/Moscow")),
                    ZonedDateTimes.of(2200, 12, 31, 23, 59, 59, 999999999, ZoneIds.of("Z"))),
            emptyList(),

            "",
            randomString(10240, random),
            listOf("String", "Строка", "ライン", "线", "שורה"),
            listOf("", " ", randomString(1, random), randomString(128, random)),

            ByteArray(0),
            randomBytes(10240, random),
            listOf(byteArrayOf(1), byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE)),
            listOf(byteArrayOf(), randomBytes(1, random), randomBytes(128, random)),

            country,
            country,
            listOf(country, Country(null, "Country3", 2)),
            emptyList(),

            country,
            country,
            listOf(country),
            listOf(country),

            null
    )
    bomb.optBomb = createBombWithNulls()
    bomb.optBomb!!.optBomb = bomb
    return bomb
}

fun createBombWithNulls(): Bomb {
    val country = Country(0, "Country", 0)
    val bomb = Bomb(
            Gid(2, 102).value(),

            true,
            null,
            listOf(true, false, true),
            null,
            false,
            null,
            listOf(false, true, false),
            null,

            0,
            null,
            listOf(-128, -1, 0, 1, 127),
            null,

            0,
            null,
            listOf(Int.MIN_VALUE, 0, Int.MAX_VALUE),
            null,

            0,
            null,
            listOf(Long.MIN_VALUE, 0, Long.MAX_VALUE),
            null,

            Instants.now(),
            null,
            listOf(Instants.ofEpochMilli(Long.MIN_VALUE), Instants.ofEpochMilli(0), Instants.ofEpochMilli(Long.MAX_VALUE)),
            null,


            BigDecimal(0),
            null,
            listOf(BigDecimal(Long.MAX_VALUE).plus(BigDecimal(1)), BigDecimal(Long.MIN_VALUE),
                    BigDecimal(Long.MAX_VALUE), BigDecimal(Long.MAX_VALUE).plus(BigDecimal(1))),
            null,

            ZonedDateTimes.ofInstant(Instants.ofEpochMilli(0), ZoneIds.of("UTC")),
            null,
            listOf(ZonedDateTimes.now(),
                    ZonedDateTimes.ofInstant(Instants.ofEpochMilli(Long.MAX_VALUE), ZoneIds.of("UTC")),
                    ZonedDateTimes.of(-2200, 1, 1, 0, 0, 0, 0, ZoneIds.of("+01:00")),
                    ZonedDateTimes.now(ZoneIds.of("Europe/Moscow")),
                    ZonedDateTimes.of(2200, 12, 31, 23, 59, 59, 999999999, ZoneIds.of("Z"))),
            null,

            "",
            null,
            listOf("String", "Строка", "ライン", "线", "שורה"),
            null,

            ByteArray(0),
            null,
            listOf(byteArrayOf(1), byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE)),
            null,

            country,
            null,
            listOf(country, Country(null, "Country2", 2)),
            null,

            country,
            null,
            listOf(country),
            null,

            null
    )
    bomb.optBomb = bomb
    return bomb
}

fun randomString(count: Int, random: Random) = String(CharArray(count) { (('a'..'z').toList() + ('A'..'Z').toList() + ('0'..'9').toList()).random(random) })

fun <T> List<T>.random(random: Random) = this[random.nextInt(this.size)]

fun randomBytes(count: Int, random: Random) = ByteArray(count) { Byte.MIN_VALUE.plus(random.nextInt(Byte.MAX_VALUE * 2 + 1)).toByte() }

