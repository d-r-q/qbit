package qbit.platform

expect fun currentTimeMillis(): Long

expect abstract class MessageDigest {
    fun digest(input: ByteArray): ByteArray
}

expect object MessageDigests {
    fun getInstance(algorithm: String): MessageDigest
}

expect fun assert(boolean: Boolean)