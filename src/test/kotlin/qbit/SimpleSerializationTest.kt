package qbit

import org.junit.Assert.*
import org.junit.Test
import qbit.serialization.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

class SimpleSerializationTest {

    private val intValues: List<Int> = listOf(0, Int.MAX_VALUE, Int.MIN_VALUE, Byte.MAX_VALUE.toInt(), Byte.MIN_VALUE.toInt())
    private val longValues: List<Long> = listOf(Long.MAX_VALUE, Long.MIN_VALUE, *(intValues.map { it.toLong() }.toTypedArray()))

    @Test
    fun testReadLong() {
        testValues(longValues, ::serializeLong, ::readLong)
    }

    @Test
    fun testReadInt() {
        testValues(intValues, ::serializeInt, ::readInt)
    }

    @Test
    fun testDeserializeLong() {
        testValues(longValues, { it -> serialize(it) }, { it -> deserialize(it, LongMark) })
    }

    @Test
    fun testDeserializeInt() {
        testValues(intValues, { it -> serialize(it) }, { it -> deserialize(it, IntMark) })
    }

    private fun <T> testValues(values: List<T>, s: (T) -> ByteArray, r: (InputStream) -> Try<T>) {
        for (v in values) {
            assertEquals(v, r(ByteArrayInputStream(s(v))).res)
        }
    }

    @Test
    fun testN() {
        assertArrayEquals(nullHash, deserialize(ByteArrayInputStream(serialize(NodeRef(nullHash))), NodeMark).res)
        val randomBytes = randomBytes(HASH_LEN)
        assertArrayEquals(randomBytes, deserialize(ByteArrayInputStream(serialize(NodeRef(randomBytes))), NodeMark).res)

        val fewBytes = randomBytes(HASH_LEN - 1)
        val res = deserialize(ByteArrayInputStream(serialize(NodeRef(fewBytes))), NodeMark)
        assertTrue(res.its<DeserializationIOErr>())
    }

    @Test
    fun testByteArray() {
        val random = randomBytes()
        assertArrayEquals(random, deserialize(ByteArrayInputStream(serialize(random)), BytesMark).res)

        val twoBytes = byteArrayOf('B'.toByte(), 0, 0, 0, 3, 0, 0)
        val res = deserialize(ByteArrayInputStream(twoBytes), BytesMark)
        assertTrue(res.its<DeserializationIOErr>())
    }

    @Test
    fun testString() {
        val random = randomString()
        assertEquals(random, deserialize(ByteArrayInputStream(serialize(random)), StringMark).res)
    }

    @Test
    fun testRoot() {
        val iid = IID(1, 4)
        val root = Root(DbUuid(iid), System.currentTimeMillis(), NodeData(arrayOf(Fact(EID(iid, 1), "test", 0))))
        val res = SimpleSerialization.deserializeNode(ByteArrayInputStream(SimpleSerialization.serializeNode(root))).res
        assertArrayEquals(root.hash, res.hash)
        assertEquals(root.source, res.source)
        assertEquals(root.timestamp, res.timestamp)
        assertEquals(root.data.trx[0].entityId, res.data.trx[0].entityId)
        assertEquals(root.data.trx[0].attribute, res.data.trx[0].attribute)
        assertEquals(root.data.trx[0].value, res.data.trx[0].value)
    }

    @Test
    fun testLeaf() {
        val iid = IID(0, 4)
        val root = Leaf(NodeRef(randomBytes(HASH_LEN)), DbUuid(iid), System.currentTimeMillis(), NodeData(arrayOf(Fact(EID(iid, 1), "test", 0))))
        val res = SimpleSerialization.deserializeNode(ByteArrayInputStream(SimpleSerialization.serializeNode(root))).res as Leaf
        assertArrayEquals(root.hash, res.hash)
        assertArrayEquals(root.parent.hash, res.parent.hash)
        assertEquals(root.source, res.source)
        assertEquals(root.timestamp, res.timestamp)
        assertEquals(root.data.trx[0].entityId, res.data.trx[0].entityId)
        assertEquals(root.data.trx[0].attribute, res.data.trx[0].attribute)
        assertEquals(root.data.trx[0].value, res.data.trx[0].value)
    }

    @Test
    fun testMerge() {
        val iid = IID(0, 4)
        val root = Merge(NodeRef(randomBytes(HASH_LEN)), NodeRef(randomBytes(HASH_LEN)), DbUuid(iid), System.currentTimeMillis(), NodeData(arrayOf(Fact(EID(iid, 1), "test", 0))))
        val res = SimpleSerialization.deserializeNode(ByteArrayInputStream(SimpleSerialization.serializeNode(root))).res as Merge
        assertArrayEquals(root.parent1.hash, res.parent1.hash)
        assertArrayEquals(root.parent2.hash, res.parent2.hash)
        assertEquals(root.source, res.source)
        assertEquals(root.timestamp, res.timestamp)
        assertEquals(root.data.trx[0].entityId, res.data.trx[0].entityId)
        assertEquals(root.data.trx[0].attribute, res.data.trx[0].attribute)
        assertEquals(root.data.trx[0].value, res.data.trx[0].value)
    }

    private fun randomBytes(count: Int = Random().nextInt(1025)) = ByteArray(count) { Byte.MIN_VALUE.plus(Random().nextInt(Byte.MAX_VALUE * 2 + 1)).toByte() }

    private fun randomString(count: Int = Random().nextInt(1025)) = String(CharArray(count) { (('a'..'z').toList() + ('A'..'Z').toList() + ('0'..'9').toList()).random() })

    private fun <T> List<T>.random() = this[Random().nextInt(this.size)]
}