package qbit.serialization

import qbit.*
import java.io.EOFException
import java.io.InputStream

object SimpleSerialization : Serialization {

    override fun serializeNode(n: NodeVal): ByteArray = when (n) {
        is Root -> serializeNode(NodeRef(nullHash), NodeRef(nullHash), n.source, n.timestamp, n.data)
        is Leaf -> serializeNode(NodeRef(nullHash), n.parent, n.source, n.timestamp, n.data)
        is Merge -> serializeNode(n.parent1, n.parent2, n.source, n.timestamp, n.data)
    }

    override fun serializeNode(parent1: Node, parent2: Node, source: DbUuid, timestamp: Long, data: NodeData) = serialize(parent1, parent2, source, timestamp, data)

    override fun deserializeNode(ins: InputStream): NodeVal {
        val parent1 = deserialize(ins, NodeMark)
        val parent2 = deserialize(ins, NodeMark)
        val iid = deserialize(ins, IntMark)
        val instanceBits = deserialize(ins, ByteMark)
        val timestamp = deserialize(ins, LongMark)
        val factsCount = deserialize(ins, IntMark)
        val facts = (1..factsCount).asSequence().map {
            val eid = deserialize(ins, LongMark)
            val attr = deserialize(ins, StringMark)
            val value = deserialize<Any>(ins)
            Fact(EID(eid), attr, value)
        }
        val nodeData = NodeData(facts.toList().toTypedArray())
        return when {
            parent1.contentEquals(nullHash) && parent2.contentEquals(nullHash) -> Root(DbUuid(IID(iid, instanceBits)), timestamp, nodeData)
            parent1.contentEquals(nullHash) && !parent2.contentEquals(nullHash) -> Leaf(NodeRef(parent2), DbUuid(IID(iid, instanceBits)), timestamp, nodeData)
            !parent1.contentEquals(nullHash) && !parent2.contentEquals(nullHash) -> Merge(NodeRef(parent1), NodeRef(parent2), DbUuid(IID(iid, instanceBits)), timestamp, nodeData)
            else -> throw DeserializationException("Corrupted node data: parent1: $parent1, parent2: $parent2")
        }
    }
}

// Serialization

internal fun serialize(vararg anys: Any): ByteArray {
    val bytes = anys.map { a ->
        when (a) {
            is Node -> byteArray('N', a.hash)
            is DbUuid -> byteArray(serialize(a.iid.value), serialize(a.iid.instanceBits))
            is Byte -> byteArray('b', a)
            is Int -> byteArray('I', serializeInt(a))
            is Long -> byteArray('L', serializeLong(a))
            is String -> byteArray('S', serializeInt(a.toByteArray().size), a.toByteArray(Charsets.UTF_8))
            is NodeData -> byteArray(serialize(a.trx.size), *a.trx.map { serialize(it) }.toTypedArray())
            is Fact -> serialize(a.entityId, a.attribute, a.value)
            is EID -> byteArray('L', serializeLong(a.value()))
            is ByteArray -> byteArray('B', serializeInt(a.size), a)
            else -> throw AssertionError("Should never happen, a is $a")
        }
    }
    return byteArray(*bytes.toTypedArray())
}

internal fun byteArray(vararg parts: Any): ByteArray = ByteArray(parts.sumBy { size(it) }) { idx ->
    var ci = idx
    val it = parts.iterator()
    var part = it.next()
    while (ci >= size(part)) {
        ci -= size(part)
        part = it.next()
    }
    when (part) {
        is ByteArray -> part[ci]
        is Byte -> part
        is Char -> String(charArrayOf(part)).toByteArray()[ci]
        else -> throw AssertionError("Should never happe, part is $part")
    }
}

internal fun size(v: Any): Int = when (v) {
    is ByteArray -> v.size
    is Byte -> 1
    is Char -> String(charArrayOf(v)).toByteArray().size
    else -> throw AssertionError("Should never happen, v is $v")
}

internal fun serializeInt(a: Int): ByteArray = byteArrayOf(byteOf(3, a), byteOf(2, a), byteOf(1, a), byteOf(0, a))

internal fun byteOf(idx: Int, i: Int) = i.shr(8 * idx).and(0xFF).toByte()

internal fun serializeLong(a: Long) = byteArrayOf(byteOf(7, a), byteOf(6, a), byteOf(5, a), byteOf(4, a),
        byteOf(3, a), byteOf(2, a), byteOf(1, a), byteOf(0, a))

internal fun byteOf(idx: Int, i: Long) = i.shr(8 * idx).and(0xFF).toByte()


// Deserialization

internal sealed class DataMark<T>

internal object ByteMark : DataMark<Byte>()
internal object IntMark : DataMark<Int>()
internal object LongMark : DataMark<Long>()
internal object BytesMark : DataMark<ByteArray>()
internal object NodeMark : DataMark<ByteArray>()
internal object StringMark : DataMark<String>()

internal val char2mark = mapOf(
        'I' to IntMark,
        'L' to LongMark,
        'B' to BytesMark,
        'N' to NodeMark,
        'S' to StringMark,
        'b' to ByteMark)

internal fun <T> deserialize(ins: InputStream, mark: DataMark<T>): T {
    val byte = ins.read()
    when {
        byte == -1 -> throw EOFException("Unexpected end of input")
        char2mark[byte.toChar()] != mark && byte.toChar() in char2mark.keys -> throw DeserializationException("Mark is ${byte.toChar()} while $mark expected")
        byte.toChar() !in char2mark -> throw DeserializationException("Unknown mark: ${byte.toChar()}")
    }
    return readMark(mark, ins)
}

internal fun <T> deserialize(ins: InputStream): T {
    val byte = ins.read()
    val mark = when {
        byte == -1 -> throw DeserializationException(cause = EOFException())
        byte.toChar() !in char2mark.keys -> throw DeserializationException("Unknown mark: ${byte.toChar()}")
        else -> char2mark[byte.toChar()] as DataMark<T>
    }
    return readMark(mark, ins)
}

private fun <T> readMark(expectedMark: DataMark<T>, ins: InputStream): T {
    return when (expectedMark) {
        ByteMark -> ins.read().toByte()
        IntMark -> readInt(ins)
        LongMark -> readLong(ins)
        NodeMark -> readBytes(ins, HASH_LEN)

        BytesMark -> readInt(ins).let { count ->
            readBytes(ins, count)
        }

        StringMark -> readInt(ins).let { count ->
            String(readBytes(ins, count), Charsets.UTF_8)
        }
    } as T
}

internal fun readBytes(ins: InputStream, count: Int): ByteArray {
    val buff = ByteArray(count)
    val len = ins.read(buff)
    if (len < count) {
        throw DeserializationException(cause = EOFException("There are not enough bytes in stream"))
    } else {
        return buff
    }
}

internal fun readInt(ins: InputStream): Int = readNumber(ins, 4) { buff ->
    buff[0].toInt().and(0xFF).shl(8 * 3) or buff[1].toInt().and(0xFF).shl(8 * 2) or
            buff[2].toInt().and(0xFF).shl(8) or buff[3].toInt().and(0xFF)
}

internal fun readLong(ins: InputStream): Long = readNumber(ins, 8) { buff ->
    buff[0].toLong().and(0xFF).shl(8 * 7) or buff[1].toLong().and(0xFF).shl(8 * 6) or
            buff[2].toLong().and(0xFF).shl(8 * 5) or buff[3].toLong().and(0xFF).shl(8 * 4) or
            buff[4].toLong().and(0xFF).shl(8 * 3) or buff[5].toLong().and(0xFF).shl(8 * 2) or
            buff[6].toLong().and(0xFF).shl(8) or buff[7].toLong().and(0xFF)
}

internal fun <T> readNumber(ins: InputStream, size: Int, parse: (ByteArray) -> T): T {
    val buff = ByteArray(size)
    val len = ins.read(buff)
    if (len < size) {
        throw EOFException("There are not enough bytes in stream")
    } else {
        return parse(buff)
    }
}
