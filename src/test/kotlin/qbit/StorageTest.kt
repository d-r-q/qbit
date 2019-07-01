package qbit

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import qbit.model.*
import qbit.ns.Namespace
import qbit.ns.root
import qbit.storage.FileSystemStorage
import qbit.storage.MemStorage
import qbit.storage.Storage
import java.nio.file.Files

class StorageTest {

    @Test
    fun testMemStorage() {
        testStorage(MemStorage())
    }

    @Test
    fun testFilesStorage() {
        val root = Files.createTempDirectory("qbit").toFile()
        val storage = FileSystemStorage(root)
        testStorage(storage)
    }

    private fun testStorage(storage: Storage) {
        val rootBytes = byteArrayOf(0, 0, 0, 0)
        val subBytes = byteArrayOf(1, 1, 1, 1)
        val rootNs = Namespace("test-root")
        val subNs = rootNs.subNs("test-sub")

        storage.add(rootNs["root-data"], rootBytes)
        storage.add(subNs["sub-data"], subBytes)

        Assert.assertArrayEquals(rootBytes, storage.load(rootNs["root-data"]))
        assertEquals(setOf(rootNs["root-data"]), storage.keys(rootNs).toSet())

        Assert.assertArrayEquals(subBytes, storage.load(subNs["sub-data"]))
        assertEquals(setOf(subNs["sub-data"]), storage.keys(subNs).toSet())
    }

    @Test
    fun testSwapHead() {
        val user = Namespace("user")
        val _id = ScalarAttr(user["val"], QString)

        val root = Files.createTempDirectory("qbit").toFile()
        val storage = FileSystemStorage(root)

        val conn = qbit(storage)
        conn.persist(_id)

        val e = Entity(_id eq "1")
        val trx = conn.trx()
        trx.persist(e)
        trx.commit()

        val loaded = storage.load(Namespace("refs")["head"])
        val hash = conn.db.hash.bytes
        Assert.assertArrayEquals(loaded, hash)
    }

    @Ignore
    fun testCopyNsConstructor() {
        val user = Namespace("nodes").subNs("test")
        val _id = ScalarAttr(user["val"], QString)

        val mem = MemStorage()
        val conn = qbit(mem)
        conn.persist(_id)

        val e = Entity(_id eq "1")
        conn.persist(e)

        val rootFile = Files.createTempDirectory("qbit").toFile()
        val storage = FileSystemStorage(rootFile)
        assertTrue(mem.subNamespaces(user.parent!!) == storage.subNamespaces(user.parent!!))

    }

}