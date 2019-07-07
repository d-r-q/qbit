package qbit

import qbit.model.Entity
import qbit.model.QString
import qbit.model.ScalarAttr
import qbit.model.eq
import qbit.ns.Namespace
import qbit.ns.ns
import qbit.ns.root
import qbit.platform.Files
import qbit.storage.FileSystemStorage
import qbit.storage.MemStorage
import qbit.storage.Storage
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageTest {

    @Test
    fun testMemStorage() {
        testStorage(MemStorage())
    }

    @Test
    fun testFilesStorage() {
        // actually it compiles
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

        assertArrayEquals(rootBytes, storage.load(rootNs["root-data"]))
        assertEquals(setOf(rootNs["root-data"]), storage.keys(rootNs).toSet())

        assertArrayEquals(subBytes, storage.load(subNs["sub-data"]))
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
        conn.persist(e)

        val loaded = storage.load(Namespace("refs")["head"])
        val hash = conn.db.hash.bytes
        assertArrayEquals(loaded, hash)
    }

    @Test
    fun testCopyNsConstructor() {
        val testNs = ns("nodes")("test")
        val _id = ScalarAttr(testNs["val"], QString)

        val origin = MemStorage()
        val conn = qbit(origin)
        conn.persist(_id)

        val e = Entity(_id eq "1")
        conn.persist(e)


        // actually it compiles
        val rootFile = Files.createTempDirectory("qbit").toFile()
        val storage = FileSystemStorage(rootFile, origin)
        assertEquals(origin.subNamespaces(testNs.parent!!), storage.subNamespaces(testNs.parent!!))
        assertEquals(storage.subNamespaces(root).sortedBy { it.name }, listOf(ns("nodes"), ns("refs")).sortedBy { it.name })
    }
}