package qbit.platform

actual fun currentTimeMillis() = System.currentTimeMillis()

actual typealias MessageDigest = java.security.MessageDigest

actual object MessageDigests {
    actual fun getInstance(algorithm: String): MessageDigest {
        return MessageDigest.getInstance(algorithm)
    }
}

actual fun assert(boolean: Boolean) {
    kotlin.assert(boolean)
}