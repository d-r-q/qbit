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

    override fun deserializeNode(ins: InputStream): Try<NodeVal> {
        val parent1 = deserialize(ins, NodeMark)
        val parent2 = ifOk(parent1) { deserialize(ins, NodeMark) }
        val iid = ifOk(parent2) { deserialize(ins, IntMark) }
        val instanceBits = ifOk(iid) { deserialize(ins, ByteMark) }
        val timestamp = ifOk(instanceBits) { deserialize(ins, LongMark) }
        val factsCount = ifOk(timestamp) { deserialize(ins, IntMark) }
        val nodeData = ifOk(factsCount) { fc ->
            (1..fc).asSequence().map {
                val eid = deserialize(ins, LongMark)
                val attr = ifOk(eid) { deserialize(ins, StringMark) }
                val value = ifOk(attr) { deserialize<Any>(ins) }
                ifOk(eid, attr, value) { e, a, v -> Fact(EID(e), a, v) }
            }.flatten().mapOk { NodeData(it.toTypedArray()) }
        }
        return ifOk(parent1, parent2, iid, instanceBits, timestamp, nodeData) { p1, p2, id, ib, ts, nd ->
            when {
                p1.contentEquals(nullHash) && p2.contentEquals(nullHash) -> Root(DbUuid(IID(id, ib)), ts, nd)
                p1.contentEquals(nullHash) && !p2.contentEquals(nullHash) -> Leaf(NodeRef(p2), DbUuid(IID(id, ib)), ts, nd)
                !p1.contentEquals(nullHash) && !p2.contentEquals(nullHash) -> Merge(NodeRef(p1), NodeRef(p2), DbUuid(IID(id, ib)), ts, nd)
                else -> throw AssertionError()
            }
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

internal fun <T> deserialize(ins: InputStream, mark: DataMark<T>): Try<T> {
    val markByte = Try<Int> { ins.read() }
    val expectedMark = markByte.ifOkTry { byte ->
        when {
            byte == -1 -> err(DeserializationIOErr(cause = EOFException()))
            char2mark[byte.toChar()] == mark -> ok(byte)
            byte.toChar() in char2mark.keys -> err(DeserializationUnexpectedMarkErr("Mark is ${byte.toChar()} while $mark expected"))
            else -> err(DeserializationUnknownMarkErr("Unknown mark: ${byte.toChar()}"))
        }
    }
    return expectedMark.ifOkTry { em -> readMark(mark, ins) }
}

internal fun <T> deserialize(ins: InputStream): Try<T> {
    return Try<Int> { ins.read() }
            .ifOkTry { byte ->
                when {
                    byte == -1 -> err(DeserializationIOErr(cause = EOFException()))
                    byte.toChar() in char2mark.keys -> ok(char2mark[byte.toChar()]!! as DataMark<T>)
                    else -> err(DeserializationUnknownMarkErr("Unknown mark: ${byte.toChar()}"))
                }
            }
            .ifOkTry { expectedMark ->
                readMark(expectedMark, ins)
            }
}

private fun <T> readMark(expectedMark: DataMark<T>, ins: InputStream): Try<T> {
    return when (expectedMark) {
        ByteMark -> Try { ins.read().toByte() }.mapErr { DeserializationIOErr(cause = it) }
        IntMark -> readInt(ins)
        LongMark -> readLong(ins)
        NodeMark -> readBytes(ins, 32)

        BytesMark -> readInt(ins).ifOkTry { count ->
            readBytes(ins, count)
        }

        StringMark -> readInt(ins).ifOkTry { count ->
            readBytes(ins, count)
                    .mapOk { String(it, Charsets.UTF_8) }
        }
    } as Try<T>
}

internal fun readBytes(ins: InputStream, count: Int): Try<ByteArray> =
        TTry {
            val buff = ByteArray(count)
            val len = ins.read(buff)
            if (len < count) {
                err(DeserializationIOErr(cause = EOFException("There are not enough bytes in stream")))
            } else {
                ok(buff)
            }
        }

internal fun readInt(ins: InputStream): Try<Int> = readNumber(ins, 4) { buff ->
    buff[0].toInt().and(0xFF).shl(8 * 3) or buff[1].toInt().and(0xFF).shl(8 * 2) or
            buff[2].toInt().and(0xFF).shl(8) or buff[3].toInt().and(0xFF)
}

internal fun readLong(ins: InputStream): Try<Long> = readNumber(ins, 8) { buff ->
    buff[0].toLong().and(0xFF).shl(8 * 7) or buff[1].toLong().and(0xFF).shl(8 * 6) or
            buff[2].toLong().and(0xFF).shl(8 * 5) or buff[3].toLong().and(0xFF).shl(8 * 4) or
            buff[4].toLong().and(0xFF).shl(8 * 3) or buff[5].toLong().and(0xFF).shl(8 * 2) or
            buff[6].toLong().and(0xFF).shl(8) or buff[7].toLong().and(0xFF)
}

internal fun <T> readNumber(ins: InputStream, size: Int, c: (ByteArray) -> T): Try<T> =
        TTry {
            val buff = ByteArray(size)
            val len = ins.read(buff)
            if (len < size) {
                err(EOFException("There are not enough bytes in stream"))
            } else {
                ok(c(buff))
            }
        }

