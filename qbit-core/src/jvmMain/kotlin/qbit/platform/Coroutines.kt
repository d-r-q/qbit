package qbit.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

actual fun createSingleThreadCoroutineDispatcher(name: String): CoroutineDispatcher {
    return Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}