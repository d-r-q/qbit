package qbit.platform

actual typealias MessageDigest = java.security.MessageDigest

actual object MessageDigests {
    actual fun getInstance(algorithm: String): MessageDigest {
        return MessageDigest.getInstance(algorithm)
    }
}
