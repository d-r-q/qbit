package qbit.platform

import kotlinx.io.core.Input
import kotlinx.io.core.Output

expect fun ByteArray.asInput(): Input

expect fun fileOutput(file: File): FileOutput

interface FileOutput : Output {

    val fd: FileDescriptor

}

