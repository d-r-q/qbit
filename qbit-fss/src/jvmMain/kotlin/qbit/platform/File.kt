package qbit.platform

import io.ktor.utils.io.core.Output
import io.ktor.utils.io.streams.asOutput
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.readBytes as readBytesImpl
import kotlin.io.resolve as resolveImpl

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

actual fun File.listFiles(action: ((File) -> Boolean)): Array<File>? {
    return this.listFiles { f -> action(f) }
}

actual typealias FileDescriptor = java.io.FileDescriptor

actual typealias Path = java.nio.file.Path

actual object Files {
    actual fun createTempDirectory(prefix: String): Path = Files.createTempDirectory(prefix)
}

actual fun fileOutput(file: File): QOutput {
    val fos = FileOutputStream(file)
    return FileOutputImpl(fos, fos.asOutput(), fos.fd)
}

class FileOutputImpl(private val fos: FileOutputStream, out: Output, override val fd: FileDescriptor) : QOutput, Output by out {

    override fun writeFully(data: ByteArray) {
        fos.write(data)
    }

}
