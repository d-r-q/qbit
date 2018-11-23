package qbit


val enabled = try {
    assert(false)
    false
} catch (e: AssertionError) {
    true
}

inline fun assert(body: () -> Boolean) {
    if (enabled) {
        assert(body())
    }

}