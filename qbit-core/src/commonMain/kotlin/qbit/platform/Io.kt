package qbit.platform

import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.Input

fun ByteArray.asInput(): Input {
    return ByteReadPacket(this, 0, this.size)
}
