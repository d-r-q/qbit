package qbit

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.ns.Namespace
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

}