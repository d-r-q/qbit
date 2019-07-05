package qbit.platform

expect fun currentTimeMillis(): Long

expect fun String.getByteArray(): ByteArray

expect fun Int.toHexString(): String

expect abstract class MessageDigest {
    fun digest(input: ByteArray): ByteArray
}

expect object MessageDigests {
    fun getInstance(algorithm: String): MessageDigest
}