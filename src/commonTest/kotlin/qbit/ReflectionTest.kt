@file:Suppress("UNUSED_PARAMETER")

package qbit

import qbit.model.Gid
import qbit.model.QTombstone
import qbit.model.gid
import qbit.platform.*
import qbit.reflection.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

data class LList(val next: LList?)

data class AllTypes(val bool: Boolean, val boolList: List<Boolean>,
                    val byte: Byte, val byteList: List<Byte>,
                    val int: Int, val intList: List<Int>,
                    val long: Long, val longList: List<Long>,
                    val inst: Instant, val instList: List<Instant>,
                    val dec: BigDecimal, val decList: List<BigDecimal>,
                    val dateTime: ZonedDateTime, val dateTimeList: List<ZonedDateTime>,
                    val str: String, val strList: List<String>,
                    val bytes: ByteArray, val bytesList: List<ByteArray>,
                    val ref: Any, val refList: List<Any>,
                    val gid: Gid, val gidList: List<Gid>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AllTypes

        if (bool != other.bool) return false
        if (boolList != other.boolList) return false
        if (byte != other.byte) return false
        if (byteList != other.byteList) return false
        if (int != other.int) return false
        if (intList != other.intList) return false
        if (long != other.long) return false
        if (longList != other.longList) return false
        if (inst != other.inst) return false
        if (instList != other.instList) return false
        if (dec != other.dec) return false
        if (decList != other.decList) return false
        if (dateTime != other.dateTime) return false
        if (dateTimeList != other.dateTimeList) return false
        if (str != other.str) return false
        if (strList != other.strList) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (bytesList != other.bytesList) return false
        if (ref != other.ref) return false
        if (refList != other.refList) return false
        if (gid != other.gid) return false
        if (gidList != other.gidList) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bool.hashCode()
        result = 31 * result + boolList.hashCode()
        result = 31 * result + byte
        result = 31 * result + byteList.hashCode()
        result = 31 * result + int
        result = 31 * result + intList.hashCode()
        result = 31 * result + long.hashCode()
        result = 31 * result + longList.hashCode()
        result = 31 * result + inst.hashCode()
        result = 31 * result + instList.hashCode()
        result = 31 * result + dec.hashCode()
        result = 31 * result + decList.hashCode()
        result = 31 * result + dateTime.hashCode()
        result = 31 * result + dateTimeList.hashCode()
        result = 31 * result + str.hashCode()
        result = 31 * result + strList.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + bytesList.hashCode()
        result = 31 * result + ref.hashCode()
        result = 31 * result + refList.hashCode()
        result = 31 * result + gid.hashCode()
        result = 31 * result + gidList.hashCode()
        return result
    }

}

private data class SubSecondaryTest(val prop1: String, val prop2: String) {

    constructor(prop1: String) : this(prop1, "prop2")

}

private data class RenamedSecondaryTest(val prop1: String, val prop2: String) {

    constructor(notAprop1: String, notAprop2: Int) : this(notAprop1, notAprop2.toString())

    var notAprop1: String = ""

    var notAprop2: Int = 1

}

private data class CustomCopy(val prop1: String, val prop2: String) {

    fun copy(prop1: Int = 1, prop2: String = this.prop2): CustomCopy {
        return CustomCopy("", "")
    }
}

class ReflectionTest {

    @Test
    fun `default should correctly handle cycles with optional refs`() {
        val default = default(LList::class)
        assertNotNull(default)
    }

    @Test
    fun `default should correctly handle all supported scalar data types`() {
        val default = default(AllTypes::class)
        assertEquals(false, default.bool)
        assertEquals(emptyList(), default.boolList)
        assertEquals(0, default.byte)
        assertEquals(emptyList(), default.byteList)
        assertEquals(0, default.int)
        assertEquals(emptyList(), default.intList)
        assertEquals(0, default.long)
        assertEquals(emptyList(), default.longList)
        assertEquals(Instants.ofEpochMilli(0), default.inst)
        assertEquals(emptyList(), default.instList)
        assertEquals(BigDecimal(0), default.dec)
        assertEquals(emptyList(), default.decList)
        assertEquals(ZonedDateTimes.of(0, 1, 1, 0, 0, 0, 0, ZoneIds.of("UTC")), default.dateTime)
        assertEquals(emptyList(), default.dateTimeList)
        assertEquals("", default.str)
        assertEquals(emptyList(), default.strList)
        assertArrayEquals(byteArrayOf(), default.bytes)
        assertEquals(emptyList(), default.bytesList)
        assertNotNull(default.ref)
        assertEquals(emptyList(), default.refList)
        assertEquals(Gid(0), default.gid)
        assertEquals(emptyList(), default.gidList)
    }

    @Test
    fun `test find mutable properties`() {
        val mutProps = findMutableProperties(Scientist::class)
        assertEquals(1, mutProps.size)
        assertEquals(Scientist::reviewer, mutProps[0])
    }

    @Test
    fun `test find primary constructor of type with single constructor`() {
        assertNotNull(findPrimaryConstructor(Scientist::class))
    }

    @Test
    fun `test find primary constructor of type with secondary constructor with lesser paramas`() {
        val constr = findPrimaryConstructor(SubSecondaryTest::class)
        assertNotNull(constr)
        assertEquals(2, constr.parameters.size)
    }

    @Test
    fun `test find primary constructor of type with secondary constructor with param names matches simple properties`() {
        val constr = findPrimaryConstructor(RenamedSecondaryTest::class)
        assertNotNull(constr)
        assertEquals(2, constr.parameters.size)
        assertEquals(String::class, constr.parameters[0].type.classifier)
        assertEquals(String::class, constr.parameters[1].type.classifier)
    }

    @Test
    fun `test find primary constructor of type with copy with the same param names`() {
        val constr = findPrimaryConstructor(CustomCopy::class)
        assertNotNull(constr)
        assertEquals(2, constr.parameters.size)
        assertEquals(String::class, constr.parameters[0].type.classifier)
        assertEquals(String::class, constr.parameters[1].type.classifier)
    }

    @Test
    fun `Test find setable props`() {
        val setableProps = setableProps(Scientist::class)
        assertEquals(1, setableProps.size)
        assertEquals("reviewer", setableProps[0].name)
    }

    @Test
    fun `Test find property for attr`() {
        assertEquals("country", Scientist::class.propertyFor(Scientists.country)?.name)
    }

    @Test
    fun `Test gid retrieving from Tombstone`(){
        assertEquals(Gid(1, 1), (QTombstone(Gid(1, 1)) as Any).gid)
    }

}