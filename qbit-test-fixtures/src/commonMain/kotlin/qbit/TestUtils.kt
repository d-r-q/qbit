package qbit

import qbit.test.model.Bomb
import qbit.test.model.Country
import kotlin.random.Random
import kotlin.test.assertEquals

fun Gid(iid: Int, eid: Int) =
    (iid.toLong() shl 32) or eid.toLong()

fun createBombWithoutNulls(
    referredBombGid: Long,
    countries: List<Country> = listOf(
        Country(Gid(3, 2), "Country", 0),
        Country(null, "Country2", 2),
        Country(null, "Country3", 2)
    ),
    gid: Long? = null
): Bomb {
    val bomb = Bomb(
        gid,

        bool = true,
        optBool = false,
        boolList = listOf(true, false, true),
        boolListOpt = emptyList(),
        mutBool = false,
        mutOptBool = false,
        mutBoolList = listOf(false, true, false),
        mutBoolListOpt = listOf(true),

        byte = 0,
        optByte = 1,
        byteList = listOf(-128, -1, 0, 1, 127),
        byteListOpt = emptyList(),

        int = 0,
        optInt = 1,
        intList = listOf(Int.MIN_VALUE, 0, Int.MAX_VALUE),
        intListOpt = listOf(2),

        long = 0,
        optLong = -1024,
        longList = listOf(Long.MIN_VALUE, 0, Long.MAX_VALUE),
        longListOpt = listOf(1024),

        str = "",
        optStr = randomString(10240, random),
        strList = listOf("String", "Строка", "ライン", "线", "שורה"),
        strListOpt = listOf(
            "", " ",
            randomString(1, random),
            randomString(128, random)
        ),

        bytes = ByteArray(0),
        optBytes = randomBytes(10240, random),
        bytesList = listOf(byteArrayOf(1), byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE)),
        bytesListOpt = listOf(
            byteArrayOf(),
            randomBytes(1, random),
            randomBytes(128, random)
        ),

        country = countries[0],
        optCountry = countries[0],
        countiesList = listOf(Country(Gid(3, 1), "Country1", 0),countries[2]),
        countriesListOpt = emptyList(),

        mutCountry = countries[0],
        mutOptCountry = countries[0],
        mutCountriesList = countries.take(1),
        mutCountriesListOpt = countries.take(1),

        optBomb = null
    )
    bomb.optBomb = createBombWithNulls(referredBombGid, countries)
    return bomb
}

fun createBombWithNulls(
    gid: Long,
    countries: List<Country> = listOf(Country(Gid(3, 2), "Country", 0), Country(null, "Country2", 2))
): Bomb {
    return Bomb(
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

        countries[0],
        null,
        countries,
        null,

        countries[0],
        null,
        countries.take(1),
        null,

        null
    )
}

val random = Random(1)

fun randomString(count: Int, random: Random) = CharArray(count) { (('a'..'z').toList() + ('A'..'Z').toList() + ('0'..'9').toList()).random(random) }.concatToString()

fun <T> List<T>.random(random: Random) = this[random.nextInt(this.size)]

fun randomBytes(count: Int, random: Random) = ByteArray(count) { Byte.MIN_VALUE.plus(random.nextInt(Byte.MAX_VALUE * 2 + 1)).toByte() }

fun assertArrayEquals(arr1: ByteArray?, arr2: ByteArray?) {
    arr1!!; arr2!!
    assertEquals(arr1.size, arr2.size)
    (arr1 zip arr2).forEach { assertEquals(it.first, it.second) }
}
