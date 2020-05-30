package qbit.platform

actual fun <T> runBlocking(body: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking {
        body()
    }
}