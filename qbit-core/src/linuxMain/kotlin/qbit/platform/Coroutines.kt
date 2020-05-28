package qbit.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

actual fun createSingleThreadCoroutineDispatcher(name: String): CoroutineDispatcher {
    return WorkerDispatcher(Worker.start(name = name))
}

class WorkerDispatcher(private val worker: Worker) : CoroutineDispatcher() {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        worker.execute(TransferMode.SAFE, { block }) {
            it.run()
        }
    }

}