package qbit.storage

import qbit.platform.Files
import qbit.spi.Storage


class FileStorageTest : StorageTest() {

    override fun storage(): Storage {
        // Actually it compiles
        val rootFile = Files.createTempDirectory("qbit").toFile()
        return FileSystemStorage(rootFile)
    }

}