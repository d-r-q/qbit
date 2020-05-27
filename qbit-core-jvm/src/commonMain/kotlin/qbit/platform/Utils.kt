package qbit.platform


expect abstract class MessageDigest {
    fun digest(input: ByteArray): ByteArray
}

expect object MessageDigests {
    fun getInstance(algorithm: String): MessageDigest
}
