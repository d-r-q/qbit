package qbit.storage

import qbit.assertArrayEquals
import qbit.ns.Namespace
import qbit.ns.ns
import qbit.ns.root
import qbit.serialization.Storage
import qbit.serialization.copyStorage
import qbit.setupTestSchema
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class StorageTest {

    abstract fun storage(): Storage

    @Test
    fun testStorage() {
        val storage = storage()
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
    fun testCopyNsConstructor() {
        val testNs = ns("nodes")("test")

        val origin = MemStorage()
        setupTestSchema(origin)

        // actually it compiles
        val storage = storage()
        copyStorage(origin, storage)
        assertEquals(origin.subNamespaces(testNs.parent!!), storage.subNamespaces(testNs.parent!!))
        assertEquals(storage.subNamespaces(root).sortedBy { it.name }, listOf(ns("nodes"), ns("refs")).sortedBy { it.name })
    }

}