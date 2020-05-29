package qbit.storage

import qbit.assertArrayEquals
import qbit.ns.root
import qbit.platform.runBlocking
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen
import kotlin.test.Test


class SerializedStorageTest {

    private val key = root["aKey"]

    private val value = byteArrayOf(1, 2, 3)

    @Test
    fun `Test access to serialized storage via different workers`() {
        runBlocking {
            // Given a serialized storage and two workers
            val storage = SerializedStorage(MemStorage())
            val writer = Worker.start(name = "Writer")
            val reader = Worker.start(name = "Reader")
            storage.freeze()
            key.freeze()
            value.freeze()

            // When writer is store some data in storage
            writer.execute(TransferMode.SAFE, { Triple(storage, key, value) },) { (storage, key, value) ->
                runBlocking {
                    println("${key.isFrozen} ${value.isFrozen} ${value[0].isFrozen}")
                    storage.add(key, value)
                }
            }.consume {  }

            // Then reader can read it
            val loaded = reader.execute(TransferMode.SAFE, { Triple(storage, key, value) }) { (storage, key) ->
                runBlocking {
                    storage.load(key)
                }
            }.consume { it }
            assertArrayEquals(value, loaded)
        }
    }

}