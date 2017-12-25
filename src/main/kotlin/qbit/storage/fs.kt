package qbit.storage

import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class FileSystemStorage(private val root: Path) : Storage {

    @Throws(IOException::class)
    override fun store(key: Key, value: ByteArray) {
        val dir = root.resolve(key.ns.toPath()).toFile()
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw IOException("Could not create directory for namespace: ${dir.absolutePath}")
            }
        }
        val file = File(dir, key.key)
        file.writeBytes(value)
    }

    @Throws(IOException::class)
    override fun load(key: Key): ByteArray? {
        val file = root.resolve(key.ns.toPath().resolve(key.key)).toFile()
        if (!file.exists()) {
            return null
        }
        return file.readBytes()
    }

    @Throws(IOException::class)
    override fun keys(namespace: Namespace): Collection<Key> {
        val dir = root.resolve(namespace.toPath()).toFile()
        return dir.listFiles { f -> f.isFile }
                .map { namespace[it.name] }
    }

    private fun Namespace.toPath(): Path = (this.parent?.toPath() ?: Paths.get("")).resolve(this.name)

}