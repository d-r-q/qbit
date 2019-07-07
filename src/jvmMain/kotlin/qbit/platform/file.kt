package qbit.platform

import java.nio.file.Files
import kotlin.io.resolve as resolveImpl
import kotlin.io.readBytes as readBytesImpl
import kotlin.io.forEachLine as forEachLineImpl
import kotlin.io.deleteRecursively as deleteRecursivelyImpl

actual typealias File = java.io.File

actual fun File.resolve(relative: File): File {
    return this.resolveImpl(relative)
}

actual fun File.resolve(relative: String): File {
    return this.resolveImpl(relative)
}

actual fun File.readBytes(): ByteArray? {
    return this.readBytesImpl()
}

actual fun File.forEachLine(action: (line: String) -> Unit) {
    this.forEachLineImpl { action(it) }
}

actual fun File.deleteRecursively(): Boolean {
    return this.deleteRecursivelyImpl()
}

actual fun File.listFiles(action: ((File) -> Boolean)): Array<File>? {
    return this.listFiles { f -> action(f) }
}

actual typealias FileDescriptor = java.io.FileDescriptor

actual typealias Path = java.nio.file.Path

actual object Files {
    actual fun createTempDirectory(prefix: String): Path = Files.createTempDirectory(prefix)
}