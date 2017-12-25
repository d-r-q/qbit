package qbit

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.storage.FileSystemStorage
import qbit.storage.Namespace
import java.nio.file.Files

class FSStorageTest {

    @Test
    fun testFsStorage() {
        val rootBytes = byteArrayOf(0, 0, 0, 0)
        val subBytes = byteArrayOf(1, 1, 1, 1)
        val root = Files.createTempDirectory("qbit")
        val storage = FileSystemStorage(root)
        val rootNs = Namespace("test-root")
        val subNs = rootNs.subNs("test-sub")

        storage.store(rootNs["root-data"], rootBytes)
        storage.store(subNs["sub-data"], subBytes)

        Assert.assertArrayEquals(rootBytes, storage.load(rootNs["root-data"]))
        assertEquals(setOf(rootNs["root-data"]), storage.keys(rootNs).toSet())

        Assert.assertArrayEquals(subBytes, storage.load(subNs["sub-data"]))
        assertEquals(setOf(subNs["sub-data"]), storage.keys(subNs).toSet())
    }

}