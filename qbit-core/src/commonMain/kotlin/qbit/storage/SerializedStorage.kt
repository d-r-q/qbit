package qbit.storage

import kotlinx.coroutines.withContext
import qbit.ns.Key
import qbit.platform.createSingleThreadCoroutineDispatcher
import qbit.spi.Storage


class SerializedStorage(private val storage: Storage) : Storage by storage {

    private val dispatcher = createSingleThreadCoroutineDispatcher()

    override suspend fun add(key: Key, value: ByteArray) {
        withContext(dispatcher) {
            storage.add(key, value)
        }
    }

    override suspend fun overwrite(key: Key, value: ByteArray) {
        withContext(dispatcher) {
            storage.overwrite(key, value)
        }
    }

}