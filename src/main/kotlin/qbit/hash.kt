package qbit

import qbit.serialization.SimpleSerialization
import java.security.MessageDigest
import java.util.*

const val HASH_LEN = 20

val nullHash = Hash(ByteArray(HASH_LEN))

fun hash(data: ByteArray): Hash = Hash(MessageDigest.getInstance("SHA-1").digest(data))

// TODO: remove dependency to SimpleSerialization
fun hash(parent1: Hash, parent2: Hash, source: DbUuid, timestamp: Long, data: NodeData) =
        hash(SimpleSerialization.serializeNode(NodeRef(parent1), NodeRef(parent2), source, timestamp, data))

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