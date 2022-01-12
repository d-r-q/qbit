package qbit.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
actual fun createSingleThreadCoroutineDispatcher(name: String): CoroutineDispatcher {
    return kotlinx.coroutines.newSingleThreadContext(name)
}
