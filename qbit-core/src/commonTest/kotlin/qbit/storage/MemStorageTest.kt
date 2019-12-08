package qbit.storage

import qbit.spi.Storage

class MemStorageTest : StorageTest() {

    override fun storage(): Storage {
        return MemStorage()
    }

}