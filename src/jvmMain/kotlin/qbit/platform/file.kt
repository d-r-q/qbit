package qbit.platform


actual typealias File = java.io.File

actual fun File.resolveImpl(relative: File): File {
    return this.resolve(relative)
}

actual fun File.resolveImpl(relative: String): File {
    return this.resolve(relative)
}

actual fun File.readBytesImpl(): ByteArray? {
    return this.readBytes()
}

actual fun File.forEachLineImpl(action: (line: String) -> Unit) {
    this.forEachLine { action(it) }
}

actual fun File.deleteRecursivelyImpl(): Boolean {
    return this.deleteRecursively()
}
actual typealias FileFilter = java.io.FileFilter