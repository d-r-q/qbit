package qbit.platform

actual fun <T> runBlocking(body: suspend () -> T): T {
    var res: T? = null
//    val job = GlobalScope.launch {
//        res = body()
//    }
//    @Suppress("ControlFlowWithEmptyBody")
//    while (job.isActive) {
//    }
//    if (job.isCancelled) {
//        job.getCancellationException().let { throw it.cause ?: it }
//    }
    return res!!
}