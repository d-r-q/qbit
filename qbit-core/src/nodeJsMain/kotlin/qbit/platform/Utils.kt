package qbit.platform

import kotlin.js.Date

actual fun currentTimeMillis(): Long =
    Date().getTime().toLong()
