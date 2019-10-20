package qbit

import qbit.storage.MemStorage
import qbit.storage.Storage

class MemStorageTest : StorageTest() {

    override fun storage(): Storage {
        return MemStorage()
    }

}