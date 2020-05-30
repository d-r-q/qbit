package qbit.storage

import qbit.assertArrayEquals
import qbit.ns.Namespace
import qbit.ns.ns
import qbit.ns.root
import qbit.platform.runBlocking
import qbit.spi.Storage
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class StorageTest {

    abstract fun storage(): Storage

    @JsName("Test_overwrite")
    @Test
    fun `Test overwrite`() {
        runBlocking {
            // Given a storage with a value for a key
            val storage = storage()
            val key = root["key"]
            val value1 = byteArrayOf(1)
            val value2 = byteArrayOf(2)
            storage.add(key, value1)

            // When the key is overwritten
            storage.overwrite(key, value2)

            // Then consecuent load should return new value
            assertArrayEquals(value2, storage.load(key))
        }
    }

    @Test
    fun testStorage() {
        runBlocking {
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
    }

    @Test
    fun testCopyNsConstructor() {
        runBlocking {
            val testNs = ns("nodes")("test")

            val origin = MemStorage()
            // qbit(origin, testsSerialModule)

            // actually it compiles
            val storage = storage()
            CloneStorage(origin, storage)()
/*
            assertEquals(origin.subNamespaces(testNs.parent!!), storage.subNamespaces(testNs.parent!!))
            assertEquals(
                storage.subNamespaces(root).sortedBy { it.name },
                listOf(ns("nodes"), ns("refs")).sortedBy { it.name })
*/
        }
    }

}