package qbit.platform

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

actual fun <T> runBlocking(body: suspend () -> T): T {
    var res: T? = null
    var ex: Exception? = null
    val job = GlobalScope.launch {
        try {
            res = body()
        } catch (e: Exception) {
            ex = e
        }
    }
    @Suppress("ControlFlowWithEmptyBody")
    while (job.isActive) {
    }
    ex?.let {
        throw it
    }
    return res!!
}
