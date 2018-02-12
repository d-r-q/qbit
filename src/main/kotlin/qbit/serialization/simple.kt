package qbit.serialization

import qbit.*
import java.io.EOFException
import java.io.InputStream

object SimpleSerialization : Serialization {

    private val nullNode = NodeRef(nullHash)

    override fun serializeNode(n: NodeVal<Hash?>): ByteArray {
        return when (n) {
            is Root -> serializeNode(nullNode, nullNode, n.source, n.timestamp, n.data)
            is Leaf -> serializeNode(nullNode, n.parent, n.source, n.timestamp, n.data)
            is Merge -> serializeNode(n.parent1, n.parent2, n.source, n.timestamp, n.data)
        }
    }

    override fun serializeNode(parent1: Node<Hash>, parent2: Node<Hash>, source: DbUuid, timestamp: Long, data: NodeData) = serialize(parent1, parent2, source, timestamp, data)

    override fun deserializeNode(ins: InputStream): NodeVal<Hash?> {
        val parent1 = Hash(deserialize(ins, QBytes))
        val parent2 = Hash(deserialize(ins, QBytes))
        val iid = deserialize(ins, QInt)
        val instanceBits = deserialize(ins, QByte)
        val timestamp = deserialize(ins, QLong)
        val factsCount = deserialize(ins, QInt)
        val facts = (1..factsCount).asSequence().map {
            val eid = deserialize(ins, QEID)
            val attr = deserialize(ins, QString)
            val value = deserialize(ins)
            Fact(eid, attr, value)
        }
        val nodeData = NodeData(facts.toList().toTypedArray())
        return when {
            parent1 == nullHash && parent2 == nullHash -> Root(null, DbUuid(IID(iid, instanceBits)), timestamp, nodeData)
            parent1 == nullHash && parent2 != nullHash -> Leaf(null, NodeRef(parent2), DbUuid(IID(iid, instanceBits)), timestamp, nodeData)
            parent1 != nullHash && parent2 != nullHash -> Merge(null, NodeRef(parent1), NodeRef(parent2), DbUuid(IID(iid, instanceBits)), timestamp, nodeData)
            else -> throw DeserializationException("Corrupted node data: parent1: $parent1, parent2: $parent2")
        }
    }
}

// Serialization

internal fun serialize(vararg anys: Any): ByteArray {
    val bytes = anys.map { a ->
        when (a) {
            is Node<*> -> serialize(a.hash!!.bytes)
            is DbUuid -> byteArray(serialize(a.iid.value), serialize(a.iid.instanceBits))
            is Boolean -> byteArray(QBoolean.code, if (a) 1.toByte() else 0.toByte())
            is Byte -> byteArray(QByte.code, a)
            is Int -> byteArray(QInt.code, serializeInt(a))
            is Long -> byteArray(QLong.code, serializeLong(a))
            is String -> byteArray(QString.code, serializeInt(a.toByteArray(Charsets.UTF_8).size), a.toByteArray(Charsets.UTF_8))
            is NodeData -> byteArray(serialize(a.trx.size), *a.trx.map { serialize(it) }.toTypedArray())
            is Fact -> serialize(a.eid, a.attr, a.value)
            is EID -> byteArray(QEID.code, serializeLong(a.value()))
            is ByteArray -> byteArray(QBytes.code, serializeInt(a.size), a)
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
        else -> throw AssertionError("Should never happen, part is $part")
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

internal fun <T : Any> deserialize(ins: InputStream, mark: DataType<T>): T {
    val byte = ins.read()
    when {
        byte == -1 -> throw EOFException("Unexpected end of input")
        byte.toByte() != mark.code -> throw DeserializationException("Code is $byte while $mark expected")
        DataType.ofCode(byte.toByte()) == null -> throw DeserializationException("Unknown mark: ${byte.toChar()}")
    }
    return readMark(ins, mark)
}

internal fun deserialize(ins: InputStream): Any {
    val byte = ins.read()
    val mark: DataType<out Any> = when (byte) {
        -1 -> throw DeserializationException(cause = EOFException())
        else -> (DataType.ofCode(byte.toByte())) ?: throw DeserializationException("Unknown mark: ${byte.toChar()}")
    }
    return readMark(ins, mark)
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> readMark(ins: InputStream, expectedMark: DataType<T>): T {
    return when (expectedMark) {
        QBoolean -> (ins.read().toByte() == 1.toByte()) as T
        QByte -> ins.read().toByte() as T
        QInt -> readInt(ins) as T
        QLong -> readLong(ins) as T
        QBytes -> readInt(ins).let { count ->
            readBytes(ins, count) as T
        }

        QString -> readInt(ins).let { count ->
            String(readBytes(ins, count), Charsets.UTF_8) as T
        }
        QEID -> EID(readLong(ins)) as T
    }
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
