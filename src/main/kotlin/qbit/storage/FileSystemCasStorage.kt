package qbit.storage

import qbit.ns.Key
import qbit.ns.Namespace
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*


class FileSystemCasStorage(private val root: Path, private val lockEffectiveTimeMs: Long = 5 * 60 * 1000) : CasStorage {

    override fun createCasable(key: Key, value: ByteArray): Boolean {
        val dir = root.resolve(key.ns.toPath()).toFile()
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw IOException("Could not create directory for namespace: ${dir.absolutePath}")
            }
        }
        val file = File(dir, key.name)
        val versionsDir = versionsDir(key)
        if (!file.createNewFile() || !versionsDir.toFile().mkdir()) {
            return false
        }

        file.writeBytes(value)
        return true
    }

    override fun load(key: Key): ByteArray? {
        val file = root.resolve(key.ns.toPath().resolve(key.name)).toFile()
        if (!file.exists()) {
            return null
        }
        return file.readBytes()
    }

    override fun cas(key: Key, oldValue: ByteArray, newValue: ByteArray): Boolean {
        val base = root.resolve(key.ns.toPath())
        val target = base.resolve(key.name)
        val tmpFile = base.resolve(key.name + UUID.randomUUID().toString())
        val lockFile = root.resolve("." + key.name + ".lock").toFile()

        tmpFile.toFile().writeBytes(newValue)
        if (!lockFile.createNewFile()) {
            val currentTimeMillis = System.currentTimeMillis()
            val lastModified = lockFile.lastModified()
            if (currentTimeMillis - lastModified > lockEffectiveTimeMs) {
                lockFile.delete()
            }
            return false
        }
        try {

            val curValue = load(key) ?: throw AssertionError("Should never happen")

            if (!Arrays.equals(curValue, oldValue)) {
                return false
            }
            Files.move(tmpFile, target, StandardCopyOption.ATOMIC_MOVE)
            return true
        } finally {
            lockFile.delete()
        }
    }
/*    override fun cas(key: Key, oldValue: ByteArray, newValue: ByteArray): Boolean {
        val oldHash = hash(oldValue)
        val versionsDir = versionsDir(key)
        val versionFile = versionsDir.resolve(oldHash.toHexString())

        if (!versionFile.toFile().createNewFile()) {
            return false
        }

        val tmp = versionsDir.resolve(oldHash.toHexString() + "-value").toFile()

        tmp.writeBytes(newValue)

        val targetFile = root.resolve(key.ns.toPath()).resolve(key.name)
        Files.move(tmp.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        //Files.copy(targetFile, versionFile, StandardCopyOption.REPLACE_EXISTING)

        return true
    }*/

    private fun versionsDir(key: Key) = root.resolve(key.ns.toPath()).resolve("." + key.name + "-qbit-versions")

    private fun Namespace.toPath(): Path = (this.parent?.toPath() ?: Paths.get("")).resolve(this.name)

}