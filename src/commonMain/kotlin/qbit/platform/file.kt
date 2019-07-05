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
}

expect interface FileFilter

expect fun File.listFiles(action: ((File) -> Boolean)): Array<File>
expect fun File.forEachLine(action: (line: String) -> Unit)
expect fun File.resolve(relative: File): File
expect fun File.resolve(relative: String): File
expect fun File.readBytes(): ByteArray?
expect fun File.deleteRecursively(): Boolean