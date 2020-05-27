package qbit.platform

actual fun assert(condition: Boolean) {
    if (!condition) {
        throw AssertionError()
    }
}