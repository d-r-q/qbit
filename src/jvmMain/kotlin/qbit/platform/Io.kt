package qbit.platform

import kotlinx.io.core.Input
import kotlinx.io.core.Output
import kotlinx.io.streams.asInput
import kotlinx.io.streams.asOutput
import java.io.ByteArrayInputStream
import java.io.FileOutputStream

actual fun ByteArray.asInput(): Input {
    return ByteArrayInputStream(this).asInput()
}

actual fun fileOutput(file: File): FileOutput {
    val fos = FileOutputStream(file)
    return FileOutputImpl(fos.asOutput(), fos.fd)
}

class FileOutputImpl(out: Output, override val fd: FileDescriptor) : FileOutput, Output by out