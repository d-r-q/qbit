package qbit


val enabled = try {
    assert(false)
    false
} catch (e: AssertionError) {
    true
}

fun assert(body: () -> Boolean) {
    if (enabled) {
        assert(body())
    }

}