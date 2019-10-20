package qbit

import qbit.platform.assert as assertImpl

val enabled = try {
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