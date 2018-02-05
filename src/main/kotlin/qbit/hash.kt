package qbit

import java.security.MessageDigest
import java.util.*

const val HASH_LEN = 20

val nullHash = Hash(ByteArray(HASH_LEN))

fun hash(data: ByteArray): Hash = Hash(MessageDigest.getInstance("SHA-1").digest(data))

class Hash(val bytes: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hash

        if (!Arrays.equals(bytes, other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(bytes)
    }

    fun toHexString(): String {
        return bytes.joinToString("") { Integer.toHexString(it.toInt() and 0xFF) }
    }

}