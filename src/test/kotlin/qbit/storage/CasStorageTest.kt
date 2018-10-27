package qbit.storage

import org.junit.Assert.*
import org.junit.Test
import qbit.ns.root
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files


class CasStorageTest {

    @Test
    fun testConflict() {
        val storage: CasStorage = FileSystemCasStorage(Files.createTempDirectory("qbit-cas"))
        val v1 = byteArrayOf(1)
        val key = root["key"]
        storage.createCasable(key, v1)
        assertEquals(v1.contentToString(), storage.load(key)!!.contentToString())

        val v2 = byteArrayOf(2)
        assertTrue(storage.cas(key, v1, v2))
        assertEquals(v2.contentToString(), storage.load(key)!!.contentToString())

        val v2_ = byteArrayOf(3)
        assertFalse(storage.cas(key, v1, v2_))
        assertEquals(v2.contentToString(), storage.load(key)!!.contentToString())
    }

    @Test
    fun testDuplicateValue() {
        val storage: CasStorage = FileSystemCasStorage(Files.createTempDirectory("qbit-cas"))
        val v1 = byteArrayOf(1)
        val key = root["key"]
        storage.createCasable(key, v1)

        val v2 = byteArrayOf(2)
        assertTrue(storage.cas(key, v1, v2))
        assertEquals(v2.contentToString(), storage.load(key)!!.contentToString())

        val v3 = byteArrayOf(1)
        assertTrue(storage.cas(key, v2, v3))
        assertEquals(v3.contentToString(), storage.load(key)!!.contentToString())

        val v4 = byteArrayOf(1)
        assertTrue(storage.cas(key, v3, v4))
        assertEquals(v4.contentToString(), storage.load(key)!!.contentToString())
    }

    @Test
    fun testRemovalOfOutdatedLock() {
        val dir = Files.createTempDirectory("qbit-cas")
        val storage: CasStorage = FileSystemCasStorage(dir, 1000)
        val v1 = byteArrayOf(1)
        val key = root["key"]
        storage.createCasable(key, v1)
        File(dir.toFile(), ".key.lock").createNewFile()
        sleep(100)

        val v2 = byteArrayOf(2)
        assertFalse(storage.cas(key, v1, v2))
        assertEquals(v1.contentToString(), storage.load(key)!!.contentToString())

        sleep(100)

        assertFalse(storage.cas(key, v1, v2))
        assertEquals(v1.contentToString(), storage.load(key)!!.contentToString())

        sleep(1000)

        assertFalse(storage.cas(key, v1, v2))
        assertEquals(v1.contentToString(), storage.load(key)!!.contentToString())

        assertTrue(storage.cas(key, v1, v2))
        assertEquals(v2.contentToString(), storage.load(key)!!.contentToString())
    }

    @Test
    fun smokeTest() {

        val storage = FileSystemCasStorage(Files.createTempDirectory("qbit-cas"))
        val key = root["smoke"]
        storage.createCasable(key, byteArrayOf(0))
        val threads = arrayListOf<Thread>()
        for (i in 0..10) {
            val t = Thread {
                for (j in 0..10) {
                    while (true) {
                        val v = storage.load(key)!![0]
                        val newVal = v + 1
                        if (storage.cas(key, byteArrayOf(v), byteArrayOf(newVal.toByte()))) {
                            break
                        }
                    }
                }
            }
            t.start()
            threads.add(t)
        }

        threads.forEach { it.join() }
        assertEquals(121, storage.load(key)!![0].toInt())
    }
}