package qbit.platform

actual fun assert(condition: Boolean) {
    kotlin.assert(condition)
}