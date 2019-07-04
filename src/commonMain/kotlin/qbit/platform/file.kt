package qbit.platform

expect class File {
    constructor(parent: File, child: String)
    constructor(parent: String)
    fun createNewFile(): Boolean
    fun exists(): Boolean
    fun mkdirs(): Boolean
    fun mkdir(): Boolean
    fun isFile(): Boolean
    fun isDirectory(): Boolean
    fun getName(): String
    fun getAbsolutePath(): String
    fun listFiles(filter: FileFilter): Array<File>
}

expect interface FileFilter

expect fun File.forEachLineImpl(action: (line: String) -> Unit)
expect fun File.resolveImpl(relative: File): File
expect fun File.resolveImpl(relative: String): File
expect fun File.readBytesImpl(): ByteArray?
expect fun File.deleteRecursivelyImpl(): Boolean