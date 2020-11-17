package qbit.platform

import io.ktor.utils.io.core.*

fun ByteArray.asInput(): Input {
    return ByteReadPacket(this, 0, this.size)
}
