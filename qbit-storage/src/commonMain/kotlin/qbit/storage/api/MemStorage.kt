package qbit.storage.api

import qbit.storage.spi.Storage


class MemStorage : Storage {

    private val storage = HashMap<Path, ByteArray>()

    override suspend fun exists(path: Path): Boolean =
            storage.containsKey(path)

    override fun close() {
        storage.clear()
    }

}