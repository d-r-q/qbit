package qbit.platform

import io.ktor.utils.io.core.*

expect class File {
    constructor(parent: File, child: String)
    constructor(parent: String)

    fun createNewFile(): Boolean
    fun exists(): Boolean
    fun mkdirs(): Boolean
    fun isFile(): Boolean
    fun isDirectory(): Boolean
    fun getName(): String
    fun getAbsolutePath(): String
}

expect object Files {
    fun createTempDirectory(prefix: String): Path
}

expect fun File.listFiles(action: ((File) -> Boolean)): Array<File>?
expect fun File.resolve(relative: File): File
expect fun File.resolve(relative: String): File
expect fun File.readBytes(): ByteArray?

expect interface Path

expect class FileDescriptor {
    fun sync()
}

expect fun fileOutput(file: File): QOutput

interface QOutput : Output {

    val fd: FileDescriptor

    fun writeFully(data: ByteArray)

}

