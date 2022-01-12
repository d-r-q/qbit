package qbit.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers


fun createSingleThreadCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.Default