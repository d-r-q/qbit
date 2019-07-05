package qbit.platform

import kotlin.io.use as useImpl
import java.io.FileInputStream

actual typealias Closable = java.io.Closeable

actual typealias InputStream = java.io.InputStream

actual typealias FileOutputStream = java.io.FileOutputStream

actual typealias ByteArrayInputStream = java.io.ByteArrayInputStream

actual inline fun <T : FileOutputStream?, R> T.use(block: (T) -> R): R {
    return useImpl(block)
}
