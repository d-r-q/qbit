package qbit.platform

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun <T> runBlocking(body: suspend () -> T): dynamic = GlobalScope.promise { body() }