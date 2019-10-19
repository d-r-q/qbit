package qbit

import qbit.ns.ns
import qbit.ns.root
import qbit.platform.Files
import qbit.storage.FileSystemStorage
import qbit.storage.MemStorage
import kotlin.test.Test
import kotlin.test.assertEquals


class FileStorageTest : StorageTest() {

    @Test
    fun testFilesStorage() {
        // actually it compiles
        val root = Files.createTempDirectory("qbit").toFile()
        val storage = FileSystemStorage(root)
        testStorage(storage)
    }

    @Test
    fun testCopyNsConstructor() {
        val testNs = ns("nodes")("test")

        val origin = MemStorage()
        setupTestSchema(origin)

        val rootFile = Files.createTempDirectory("qbit").toFile()

        // actually it compiles
        val storage = FileSystemStorage(rootFile, origin)
        assertEquals(origin.subNamespaces(testNs.parent!!), storage.subNamespaces(testNs.parent!!))
        assertEquals(storage.subNamespaces(root).sortedBy { it.name }, listOf(ns("nodes"), ns("refs")).sortedBy { it.name })
    }

}