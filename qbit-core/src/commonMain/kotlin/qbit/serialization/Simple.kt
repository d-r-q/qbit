package qbit.serialization

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import qbit.api.gid.Gid
import qbit.api.gid.Iid
import qbit.api.model.*
import qbit.api.system.DbUuid
import qbit.platform.asInput

object SimpleSerialization : Serialization {

    private val nullNode = NodeRef(nullHash)

    override fun serializeNode(n: NodeVal<Hash?>): ByteArray {
        return when (n) {
            is Root -> serializeNode(nullNode, nullNode, nullNode, n.source, n.timestamp, n.data)
            is Leaf -> serializeNode(nullNode, nullNode, n.parent, n.source, n.timestamp, n.data)
            is Merge -> serializeNode(n.base, n.parent1, n.parent2, n.source, n.timestamp, n.data)
        }
    }

    override fun serializeNode(
        base: Node<Hash>,
        parent1: Node<Hash>,
        parent2: Node<Hash>,
        source: DbUuid,
        timestamp: Long,
        data: NodeData
    ) =
        serialize(base, parent1, parent2, source, timestamp, data)

    override fun deserializeNode(ins: Input): NodeVal<Hash?> {
        val base = Hash(deserialize(ins, QBytes) as ByteArray)
        val parent1 = Hash(deserialize(ins, QBytes) as ByteArray)
        val parent2 = Hash(deserialize(ins, QBytes) as ByteArray)
        val iid = deserialize(ins, QLong) as Long
        val instanceBits = deserialize(ins, QLong) as Long
        val timestamp = deserialize(ins, QLong) as Long
        val factsCount = deserialize(ins, QLong) as Long
        val facts = (1..factsCount).asSequence().map {
            val eid = deserialize(ins, QGid) as Gid
            val attr = deserialize(ins, QString) as String
            val value = deserialize(ins)
            Eav(eid, attr, value)
        }
        val nodeData = NodeData(facts.toList().toTypedArray())
        return when {
            parent1 == nullHash && parent2 == nullHash -> Root(
                null,
                DbUuid(Iid(iid.toInt(), instanceBits.toByte())),
                timestamp,
                nodeData
            )
            parent1 == nullHash && parent2 != nullHash -> Leaf(
                null,
                NodeRef(parent2),
                DbUuid(Iid(iid.toInt(), instanceBits.toByte())),
                timestamp,
                nodeData
            )
            base != nullHash && parent1 != nullHash && parent2 != nullHash -> Merge(
                null,
                NodeRef(base),
                NodeRef(parent1),
                NodeRef(parent2),
                DbUuid(Iid(iid.toInt(), instanceBits.toByte())),
                timestamp,
                nodeData
            )
            else -> throw DeserializationException("Corrupted node data: parent1: $parent1, parent2: $parent2")
        }
    }
}

// Serialization

internal fun serialize(vararg anys: Any): ByteArray {
    val bytes = anys.map { a ->
        if (a as? Number != null) {
            // see https://github.com/d-r-q/qbit/issues/114, https://github.com/d-r-q/qbit/issues/132
            byteArray(QLong.code, serializeLong(a.toLong()))
        } else {
            when (a) {
                is Node<*> -> serialize(a.hash!!.bytes)
                is DbUuid -> byteArray(serialize(a.iid.value), serialize(a.iid.instanceBits))
                is Boolean -> byteArray(QBoolean.code, if (a) 1.toByte() else 0.toByte())
                // see https://github.com/d-r-q/qbit/issues/114, https://github.com/d-r-q/qbit/issues/132
                is Number -> byteArray(QLong.code, a)
                is String -> byteArray(QString.code, byteArray(a))
                is NodeData -> byteArray(serialize(a.trxes.size), *a.trxes.map { serialize(it) }.toTypedArray())
                is Eav -> serialize(a.gid, a.attr, a.value)
                is Gid -> byteArray(QGid.code, serializeLong(a.value()))
                is ByteArray -> byteArray(QBytes.code, serializeLong(a.size.toLong()), a)
                else -> throw AssertionError("Should never happen, a is $a: ${a::class}")
            }
        }
    }
    return byteArray(*bytes.toTypedArray())
}

@ExperimentalIoApi
private fun byteArray(str: String): ByteArray =
    byteArray(serializeLong(str.encodeToUtf8().size.toLong()), str.encodeToUtf8())

private fun encodeToUtf8(c: Char): ByteArray =
    charArrayOf(c).concatToString().encodeToUtf8()

private val coder = HashMap<Char, ByteArray>()

internal fun byteArray(vararg parts: Any): ByteArray {
    val res = ByteArray(parts.sumBy { size(it) })
    var idx = 0
    for (part in parts) {
        when (part) {
            is ByteArray -> {
                part.copyInto(res, idx, 0, part.size)
                idx += part.size
            }
            is Byte -> {
                res[idx++] = part
            }
            is Char -> {
                val bytes = coder.getOrPut(part) { encodeToUtf8(part) }
                bytes.copyInto(res, idx, 0, bytes.size)
                idx += bytes.size
            }
        }
    }
    return res
}

internal fun size(v: Any): Int = when (v) {
    is ByteArray -> v.size
    is Byte -> 1
    is Char -> charArrayOf(v).concatToString().encodeToUtf8().size
    else -> throw AssertionError("Should never happen, v is ${v::class}")
}

internal fun serializeInt(a: Int): ByteArray = byteArrayOf(byteOf(3, a), byteOf(2, a), byteOf(1, a), byteOf(0, a))

internal fun byteOf(idx: Int, i: Int) = i.shr(8 * idx).and(0xFF).toByte()

internal fun serializeLong(a: Long) = byteArrayOf(
    byteOf(7, a), byteOf(6, a), byteOf(5, a), byteOf(4, a),
    byteOf(3, a), byteOf(2, a), byteOf(1, a), byteOf(0, a)
)

internal fun byteOf(idx: Int, i: Long) = i.shr(8 * idx).and(0xFF).toByte()


// Deserialization

@ExperimentalIoApi
internal fun <T : Any> deserialize(ins: Input, mark: DataType<T>): Any {
    val byte: Byte = ins.readByte()
    when {
        byte == (-1).toByte() -> throw EOFException("Unexpected end of input")
        byte != mark.code -> throw DeserializationException("Code is $byte while ${mark.code} expected")
        DataType.ofCode(byte) == null -> throw DeserializationException("Unknown mark: ${byte.toChar()}")
    }
    return readMark(ins, mark)
}

@ExperimentalIoApi
internal fun deserialize(ins: Input): Any {
    val mark: DataType<Any> = when (val byte = ins.readByte()) {
        (-1).toByte() -> throw DeserializationException(cause = EOFException("Unexpected end of input"))
        else -> (DataType.ofCode(byte)) ?: throw DeserializationException("Unknown mark: ${byte.toChar()}")
    }
    return readMark(ins, mark)
}

@ExperimentalIoApi
@Suppress("UNCHECKED_CAST")
private fun <T : Any> readMark(ins: Input, expectedMark: DataType<T>): Any {
    return when (expectedMark) {
        QBoolean -> (ins.readByte() == 1.toByte()) as T
        QByte, QInt, QLong, is QCounter<*> -> readLong(ins) as T

        QBytes -> readLong(ins).let { count ->
            readBytes(ins, count.toInt()) as T
        }

        QString -> readLong(ins).let { count ->
            readBytes(ins, count.toInt()).decodeUtf8() as T
        }
        QGid -> Gid(readLong(ins)) as T
        QRef -> throw AssertionError("Should never happen")
        is QList<*> -> throw AssertionError("Should never happen")
    }
}

internal fun readBytes(ins: Input, count: Int): ByteArray {
    val buff = ByteArray(count)
    val len = ins.readAvailable(buff)
    if (len < count) {
        throw DeserializationException(cause = EOFException("There are not enough bytes in stream"))
    } else {
        return buff
    }
}

internal fun readInt(ins: Input): Int = readNumber(ins, 4) { buff ->
    buff[0].toInt().and(0xFF).shl(8 * 3) or buff[1].toInt().and(0xFF).shl(8 * 2) or
            buff[2].toInt().and(0xFF).shl(8) or buff[3].toInt().and(0xFF)
}

internal fun readLong(ins: Input): Long = readNumber(ins, 8) { buff ->
    buff[0].toLong().and(0xFF).shl(8 * 7) or buff[1].toLong().and(0xFF).shl(8 * 6) or
            buff[2].toLong().and(0xFF).shl(8 * 5) or buff[3].toLong().and(0xFF).shl(8 * 4) or
            buff[4].toLong().and(0xFF).shl(8 * 3) or buff[5].toLong().and(0xFF).shl(8 * 2) or
            buff[6].toLong().and(0xFF).shl(8) or buff[7].toLong().and(0xFF)
}

internal fun <T> readNumber(ins: Input, size: Int, parse: (ByteArray) -> T): T {
    val buff = ByteArray(size)
    val len = ins.readAvailable(buff)
    if (len < size) {
        throw EOFException("There are not enough bytes in stream")
    } else {
        return parse(buff)
    }
}

@ExperimentalIoApi
private fun String.encodeToUtf8(): ByteArray {
    // single char may be encoded by up to 4 bytes in UTF-8
    val buffer = ByteArray(this.length * 4)
    val encoded: ByteReadPacket = Charset.forName("UTF-8").newEncoder().encode(this)
    val len = encoded.readAvailable(buffer)
    return buffer.copyInto(ByteArray(len), 0, 0, len)
}

@ExperimentalIoApi
private fun ByteArray.decodeUtf8(): String {
    return Charset.forName("UTF-8").newDecoder().decode(this.asInput())
}
