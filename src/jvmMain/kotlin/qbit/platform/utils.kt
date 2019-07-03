package qbit.platform

actual fun getCurrentMillis() = System.currentTimeMillis()

actual fun getByteArrayOfString(string: String) = string.toByteArray()

actual fun getHexStringOfInt(int: Int): String = Integer.toHexString(int)