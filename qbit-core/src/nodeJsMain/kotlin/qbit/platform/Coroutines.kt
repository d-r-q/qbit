package qbit.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun createSingleThreadCoroutineDispatcher(name: String): CoroutineDispatcher {
    return Dispatchers.Default
}