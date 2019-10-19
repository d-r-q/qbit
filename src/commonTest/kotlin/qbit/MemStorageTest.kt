package qbit

import qbit.storage.MemStorage
import kotlin.test.Test


class MemStorageTest : StorageTest() {

    @Test
    fun testMemStorage() {
        testStorage(MemStorage())
    }

}