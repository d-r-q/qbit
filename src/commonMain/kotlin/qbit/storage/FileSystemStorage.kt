package qbit.storage

import kotlinx.io.core.use
import kotlinx.io.core.writeFully
import kotlinx.io.errors.IOException
import qbit.ns.Namespace
import qbit.ns.Key
import qbit.QBitException
import qbit.platform.*

class FileSystemStorage(private val root: File) : Storage {

    constructor(root: File, origin: Storage) : this(root) {
        fun copyNs(ns: Namespace) {
            origin.keys(ns).forEach {
                add(it, origin.load(it)!!)
            }
            origin.subNamespaces(ns).forEach {
                copyNs(it)
            }
        }
        copyNs(qbit.ns.root)
    }

    override fun add(key: Key, value: ByteArray) {
        val dir = root.resolve(key.ns.toFile())
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw IOException("Could not create directory for namespace: ${dir.getAbsolutePath()}")
            }
        }
        val file = File(dir, key.name)
        if (!file.createNewFile()) {
            throw QBitException("Value with key $key already exists")
        }

        writeAndSync(file, value)
    }

    override fun overwrite(key: Key, value: ByteArray) {
        val file = File(root.resolve(key.ns.toFile()), key.name)
        if (!file.exists()) {
            throw QBitException("Value with key $key does not exists")
        }

        writeAndSync(file, value)
    }

    override fun load(key: Key): ByteArray? {
        val file = root.resolve(key.ns.toFile().resolve(key.name))
        if (!file.exists()) {
            return null
        }
        return file.readBytes()
    }

    override fun keys(namespace: Namespace): Collection<Key> {
        val dir = root.resolve(namespace.toFile())
        return dir.listFiles { f -> f.isFile() }
                ?.map { namespace[it.getName()] }
                ?: emptyList()
    }

    override fun subNamespaces(namespace: Namespace): Collection<Namespace> {
        val dir = root.resolve(namespace.toFile())
        return dir.listFiles { f -> f.isDirectory() }
                ?.map { namespace.subNs(it.getName()) }
                ?: emptyList()
    }

    override fun hasKey(key: Key): Boolean {
        return File(root.resolve(key.ns.toFile()), key.name).exists()
    }

    private fun Namespace.toFile(): File = (this.parent?.toFile() ?: File("")).resolve(this.name)

    private fun writeAndSync(file: File, data: ByteArray) {
        val fo = fileOutput(file)
        fo.use {
            fo.writeFully(data)
            fo.flush()
            fo.fd.sync()
        }
    }

}