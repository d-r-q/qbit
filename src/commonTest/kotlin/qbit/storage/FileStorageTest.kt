package qbit

import qbit.platform.Files
import qbit.storage.FileSystemStorage
import qbit.storage.Storage


class FileStorageTest : StorageTest() {

    override fun storage(): Storage {
        // Actually it compiles
        val rootFile = Files.createTempDirectory("qbit").toFile()
        return FileSystemStorage(rootFile)
    }

}