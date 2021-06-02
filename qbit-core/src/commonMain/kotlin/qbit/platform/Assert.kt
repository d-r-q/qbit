package qbit.platform

import qbit.platform.assert as assertImpl

expect fun assert(condition: Boolean)

expect fun assert(condition: Boolean, lazyMessage: () -> String)

var enabled = try {
    assertImpl(false)
    false
} catch (e: AssertionError) {
    true
}

inline fun assert(body: () -> Boolean) {
    if (enabled) {
        assertImpl(body())
    }
}

inline fun assert(message: String, body: () -> Boolean) {
    if (enabled) {
        assertImpl(body()) { message }
    }
}
