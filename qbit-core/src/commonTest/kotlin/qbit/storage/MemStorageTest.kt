package qbit.storage

import qbit.serialization.Storage

class MemStorageTest : StorageTest() {

    override fun storage(): Storage {
        return MemStorage()
    }

}