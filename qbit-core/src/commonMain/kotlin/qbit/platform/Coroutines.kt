package qbit.platform

import kotlinx.coroutines.CoroutineDispatcher


expect fun createSingleThreadCoroutineDispatcher(name: String = ""): CoroutineDispatcher