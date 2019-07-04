package qbit.platform

actual fun currentTimeMillis() = System.currentTimeMillis()

actual fun String.getByteArray(): ByteArray = this.toByteArray()

actual fun Int.toHexString(): String = Integer.toHexString(this)