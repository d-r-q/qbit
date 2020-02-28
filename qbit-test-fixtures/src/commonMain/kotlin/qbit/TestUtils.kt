package qbit

import qbit.test.model.Bomb
import qbit.test.model.Country
import kotlin.random.Random
import kotlin.test.assertEquals

fun Gid(iid: Int, eid: Int) =
    (iid.toLong() shl 32) or eid.toLong()

fun createBombWithoutNulls(gid: Long): Bomb {
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

        "",
        randomString(10240, random),
        listOf("String", "Строка", "ライン", "线", "שורה"),
        listOf("", " ",
            randomString(1, random),
            randomString(128, random)
        ),

        ByteArray(0),
        randomBytes(10240, random),
        listOf(byteArrayOf(1), byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE)),
        listOf(byteArrayOf(),
            randomBytes(1, random),
            randomBytes(128, random)
        ),

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
    bomb.optBomb = createBombWithNulls(gid)
    return bomb
}

fun createBombWithNulls(gid: Long): Bomb {
    val country = Country(0, "Country", 0)
    val bomb = Bomb(
        gid,

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
    return bomb
}

val random = Random(1)

fun randomString(count: Int, random: Random) = String(CharArray(count) { (('a'..'z').toList() + ('A'..'Z').toList() + ('0'..'9').toList()).random(random) })

fun <T> List<T>.random(random: Random) = this[random.nextInt(this.size)]

fun randomBytes(count: Int, random: Random) = ByteArray(count) { Byte.MIN_VALUE.plus(random.nextInt(Byte.MAX_VALUE * 2 + 1)).toByte() }

fun assertArrayEquals(arr1: ByteArray?, arr2: ByteArray?) {
    arr1!!; arr2!!
    assertEquals(arr1.size, arr2.size)
    (arr1 zip arr2).forEach { assertEquals(it.first, it.second) }
}
