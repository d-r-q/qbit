package qbit.platform

import kotlinx.io.core.Output
import kotlinx.io.streams.asOutput
import java.io.FileOutputStream
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

actual fun fileOutput(file: File): FileOutput {
    val fos = FileOutputStream(file)
    return FileOutputImpl(fos.asOutput(), fos.fd)
}

class FileOutputImpl(out: Output, override val fd: FileDescriptor) : FileOutput, Output by out
