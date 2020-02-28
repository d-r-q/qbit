package qbit.storage

import kotlinx.serialization.modules.serializersModuleOf
import qbit.assertArrayEquals
import qbit.factorization.KSFactorization
import qbit.ns.Namespace
import qbit.ns.ns
import qbit.ns.root
import qbit.qbit
import qbit.spi.Storage
import qbit.spi.copyStorage
import kotlin.test.Test
import kotlin.test.assertEquals

// It's duplication of the same class in qbit-core - study multiplatform builds and get rid of this duplication
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
        // initialize storage
        qbit(origin, KSFactorization(serializersModuleOf(mapOf()))::ksDestruct)

        // actually it compiles
        val storage = storage()
        copyStorage(origin, storage)
        assertEquals(origin.subNamespaces(testNs.parent!!), storage.subNamespaces(testNs.parent!!))
        assertEquals(storage.subNamespaces(root).sortedBy { it.name }, listOf(ns("nodes"), ns("refs")).sortedBy { it.name })
    }

}

