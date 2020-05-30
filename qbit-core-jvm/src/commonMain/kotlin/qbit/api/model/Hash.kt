package qbit.api.model

import kotlinx.serialization.Serializable
import qbit.platform.MessageDigests

const val HASH_LEN = 20

val nullHash = Hash(ByteArray(HASH_LEN))

fun hash(data: ByteArray): Hash = Hash(MessageDigests.getInstance("SHA-1").digest(data))

@Serializable
data class Hash(val bytes: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this::class != other?.let { it::class } ) return false

        other as Hash

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    fun toHexString(): String {
        return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16) }
    }

    override fun toString(): String {
        return "Hash(bytes=${bytes.contentToString()})"
    }

}