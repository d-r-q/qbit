package qbit.platform

actual fun currentTimeMillis() = System.currentTimeMillis()

actual fun String.getByteArray(): ByteArray = this.toByteArray()

actual fun Int.toHexString(): String = Integer.toHexString(this)

actual typealias MessageDigest = java.security.MessageDigest

actual object MessageDigests {
    actual fun getInstance(algorithm: String): MessageDigest {
        return MessageDigest.getInstance(algorithm)
    }
}