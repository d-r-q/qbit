package qbit.serialization.krypto

import kotlin.math.min

// the code is copied from https://github.com/korlibs/krypto

object Base64 {
    private val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    private val DECODE = IntArray(0x100).apply {
        for (n in 0..255) this[n] = -1
        for (n in 0 until TABLE.length) {
            this[TABLE[n].toInt()] = n
        }
    }

    operator fun invoke(v: String) = decodeIgnoringSpaces(v)
    operator fun invoke(v: ByteArray) = encode(v)

    fun decode(str: String): ByteArray {
        val src = ByteArray(str.length) { str[it].toByte() }
        val dst = ByteArray(src.size)
        return dst.copyOf(decode(src, dst))
    }

    fun decodeIgnoringSpaces(str: String): ByteArray {
        return decode(str.replace(" ", "").replace("\n", "").replace("\r", ""))
    }

    fun decode(src: ByteArray, dst: ByteArray): Int {
        var m = 0

        var n = 0
        while (n < src.size) {
            val d = DECODE[src.readU8(n)]
            if (d < 0) {
                n++
                continue // skip character
            }

            val b0 = DECODE[src.readU8(n++)]
            val b1 = DECODE[src.readU8(n++)]
            val b2 = DECODE[src.readU8(n++)]
            val b3 = DECODE[src.readU8(n++)]
            dst[m++] = (b0 shl 2 or (b1 shr 4)).toByte()
            if (b2 < 64) {
                dst[m++] = (b1 shl 4 or (b2 shr 2)).toByte()
                if (b3 < 64) {
                    dst[m++] = (b2 shl 6 or b3).toByte()
                }
            }
        }
        return m
    }

    @Suppress("UNUSED_CHANGED_VALUE")
    fun encode(src: ByteArray): String {
        val out = StringBuilder((src.size * 4) / 3 + 4)
        var ipos = 0
        val extraBytes = src.size % 3
        while (ipos < src.size - 2) {
            val num = src.readU24BE(ipos)
            ipos += 3

            out.append(TABLE[(num ushr 18) and 0x3F])
            out.append(TABLE[(num ushr 12) and 0x3F])
            out.append(TABLE[(num ushr 6) and 0x3F])
            out.append(TABLE[(num ushr 0) and 0x3F])
        }

        if (extraBytes == 1) {
            val num = src.readU8(ipos++)
            out.append(TABLE[num ushr 2])
            out.append(TABLE[(num shl 4) and 0x3F])
            out.append('=')
            out.append('=')
        } else if (extraBytes == 2) {
            val tmp = (src.readU8(ipos++) shl 8) or src.readU8(ipos++)
            out.append(TABLE[tmp ushr 10])
            out.append(TABLE[(tmp ushr 4) and 0x3F])
            out.append(TABLE[(tmp shl 2) and 0x3F])
            out.append('=')
        }

        return out.toString()
    }

    private fun ByteArray.readU8(index: Int): Int = this[index].toInt() and 0xFF
    private fun ByteArray.readU24BE(index: Int): Int =
        (readU8(index + 0) shl 16) or (readU8(index + 1) shl 8) or (readU8(index + 2) shl 0)
}


object Hex {
    val DIGITS = "0123456789ABCDEF"
    val DIGITS_UPPER = DIGITS.toUpperCase()
    val DIGITS_LOWER = DIGITS.toLowerCase()

    fun decodeHexDigit(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> (c - 'a') + 10
        in 'A'..'F' -> (c - 'A') + 10
        else -> error("Invalid hex digit '$c'")
    }

    operator fun invoke(v: String) = decode(v)
    operator fun invoke(v: ByteArray) = encode(v)

    fun decode(str: String): ByteArray {
        val out = ByteArray(str.length / 2)
        var m = 0
        for (n in out.indices) {
            val c0 = decodeHexDigit(str[m++])
            val c1 = decodeHexDigit(str[m++])
            out[n] = ((c0 shl 4) or c1).toByte()
        }
        return out
    }

    fun encode(src: ByteArray): String = encodeBase(src,DIGITS_LOWER)
    fun encodeLower(src: ByteArray): String = encodeBase(src, DIGITS_LOWER)
    fun encodeUpper(src: ByteArray): String = encodeBase(src, DIGITS_UPPER)

    private fun encodeBase(data: ByteArray, digits: String = DIGITS): String = buildString(data.size * 2) {
        for (n in data.indices) {
            val v = data[n].toInt() and 0xFF
            append(digits[(v ushr 4) and 0xF])
            append(digits[(v ushr 0) and 0xF])
        }
    }
}

internal fun Int.rotateLeft(bits: Int): Int = ((this shl bits) or (this ushr (32 - bits)))

internal fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) = src.copyInto(dst, dstPos, srcPos, srcPos + count)
internal fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int) = src.copyInto(dst, dstPos, srcPos, srcPos + count)

internal fun ByteArray.readU8(o: Int): Int = this[o].toInt() and 0xFF
internal fun ByteArray.readS32_be(o: Int): Int =
    (readU8(o + 3) shl 0) or (readU8(o + 2) shl 8) or (readU8(o + 1) shl 16) or (readU8(o + 0) shl 24)

open class HasherFactory(val create: () -> Hasher) {
    fun digest(data: ByteArray) = create().also { it.update(data, 0, data.size) }.digest()
}

abstract class Hasher(val chunkSize: Int, val digestSize: Int) {
    private val chunk = ByteArray(chunkSize)
    private var writtenInChunk = 0
    private var totalWritten = 0L

    fun reset(): Hasher {
        coreReset()
        writtenInChunk = 0
        totalWritten = 0L
        return this
    }

    fun update(data: ByteArray, offset: Int, count: Int): Hasher {
        var curr = offset
        var left = count
        while (left > 0) {
            val remainingInChunk = chunkSize - writtenInChunk
            val toRead = min(remainingInChunk, left)
            arraycopy(data, curr, chunk, writtenInChunk, toRead)
            left -= toRead
            curr += toRead
            writtenInChunk += toRead
            if (writtenInChunk >= chunkSize) {
                writtenInChunk -= chunkSize
                coreUpdate(chunk)
            }
        }
        totalWritten += count
        return this
    }

    fun digestOut(out: ByteArray) {
        val pad = corePadding(totalWritten)
        var padPos = 0
        while (padPos < pad.size) {
            val padSize = chunkSize - writtenInChunk
            arraycopy(pad, padPos, chunk, writtenInChunk, padSize)
            coreUpdate(chunk)
            writtenInChunk = 0
            padPos += padSize
        }

        coreDigest(out)
        coreReset()
    }

    protected abstract fun coreReset()
    protected abstract fun corePadding(totalWritten: Long): ByteArray
    protected abstract fun coreUpdate(chunk: ByteArray)
    protected abstract fun coreDigest(out: ByteArray)

    fun update(data: ByteArray) = update(data, 0, data.size)
    fun digest(): Hash = Hash(ByteArray(digestSize).also { digestOut(it) })
}

class Hash(val bytes: ByteArray)

fun ByteArray.hash(algo: HasherFactory): Hash = algo.digest(this)

abstract class SHA(chunkSize: Int, digestSize: Int) : Hasher(chunkSize, digestSize) {
    override fun corePadding(totalWritten: Long): ByteArray {
        val tail = totalWritten % 64
        val padding = (if (64 - tail >= 9) 64 - tail else 128 - tail)
        val pad = ByteArray(padding.toInt()).apply { this[0] = 0x80.toByte() }
        val bits = (totalWritten * 8)
        for (i in 0 until 8) pad[pad.size - 1 - i] = ((bits ushr (8 * i)) and 0xFF).toByte()
        return pad
    }
}

class SHA1 : SHA(chunkSize = 64, digestSize = 20) {
    companion object : HasherFactory({ SHA1() }) {
        private val H = intArrayOf(
            0x67452301L.toInt(),
            0xEFCDAB89L.toInt(),
            0x98BADCFEL.toInt(),
            0x10325476L.toInt(),
            0xC3D2E1F0L.toInt()
        )

        private const val K0020: Int = 0x5A827999L.toInt()
        private const val K2040: Int = 0x6ED9EBA1L.toInt()
        private const val K4060: Int = 0x8F1BBCDCL.toInt()
        private const val K6080: Int = 0xCA62C1D6L.toInt()
    }

    private val w = IntArray(80)
    private val h = IntArray(5)

    override fun coreReset(): Unit = run { arraycopy(H, 0, h, 0, 5) }

    init {
        coreReset()
    }

    override fun coreUpdate(chunk: ByteArray) {
        for (j in 0 until 16) w[j] = chunk.readS32_be(j * 4)
        for (j in 16 until 80) w[j] = (w[j - 3] xor w[j - 8] xor w[j - 14] xor w[j - 16]).rotateLeft(1)

        var a = h[0]
        var b = h[1]
        var c = h[2]
        var d = h[3]
        var e = h[4]

        for (j in 0 until 80) {
            val temp = a.rotateLeft(5) + e + w[j] + when (j / 20) {
                0 -> ((b and c) or ((b.inv()) and d)) + K0020
                1 -> (b xor c xor d) + K2040
                2 -> ((b and c) xor (b and d) xor (c and d)) + K4060
                else -> (b xor c xor d) + K6080
            }

            e = d
            d = c
            c = b.rotateLeft(30)
            b = a
            a = temp
        }

        h[0] += a
        h[1] += b
        h[2] += c
        h[3] += d
        h[4] += e
    }

    override fun coreDigest(out: ByteArray) {
        for (n in out.indices) out[n] = (h[n / 4] ushr (24 - 8 * (n % 4))).toByte()
    }
}

fun ByteArray.sha1() = hash(SHA1)