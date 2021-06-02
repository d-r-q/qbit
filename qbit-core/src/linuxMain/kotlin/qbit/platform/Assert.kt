package qbit.platform

actual fun assert(condition: Boolean) {
    if (!condition) {
        throw AssertionError()
    }
}

actual fun assert(condition: Boolean, lazyMessage: () -> String) {
    if (!condition) {
        throw AssertionError(lazyMessage())
    }
}