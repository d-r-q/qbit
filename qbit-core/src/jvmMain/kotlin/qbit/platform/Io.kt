package qbit.platform

import kotlinx.io.core.Input
import kotlinx.io.streams.asInput
import java.io.ByteArrayInputStream

actual fun ByteArray.asInput(): Input {
    return ByteArrayInputStream(this).asInput()
}
