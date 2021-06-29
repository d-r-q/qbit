package qbit.platform

actual fun assert(condition: Boolean) {
    kotlin.assert(condition)
}

actual fun assert(condition: Boolean, lazyMessage: () -> String) {
    kotlin.assert(condition, lazyMessage)
}
