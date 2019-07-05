package qbit.platform

expect interface Closable

expect abstract class InputStream: Closable {
    abstract fun read(): Int
    fun read(buff: ByteArray): Int
}

expect class FileOutputStream(file: File) {
    fun write(data: ByteArray)
    fun flush()
    fun getFD(): FileDescriptor
}

expect class ByteArrayInputStream(data: ByteArray): InputStream

expect inline fun <T : FileOutputStream?, R> T.use(block: (T) -> R): R