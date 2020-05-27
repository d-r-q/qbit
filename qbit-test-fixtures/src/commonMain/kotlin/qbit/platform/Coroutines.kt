package qbit.platform


expect fun <T> runBlocking(body: suspend () -> T): T
