package qbit.storage

import io.ktor.client.features.ClientRequestException
import kotlinx.coroutines.InternalCoroutinesApi
import qbit.api.QBitException
import qbit.ns.Key
import qbit.ns.Namespace
import qbit.serialization.Storage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue


class YandexDiskStorageTest {

    private val TEST_ACCESS_TOKEN = "AgAAAAA4v_tAAADLW-MJ7N5YOU1BvQAUJ3i8_XM"

    @InternalCoroutinesApi
    @Test
    fun add() {
        val yandexDiskStorage = YandexDiskStorage(TEST_ACCESS_TOKEN, YandexDiskConfig())
        val namespace = Namespace("production-test")
        val key = Key(namespace, "production-test-file1")
        val byteArray = ByteArray(1234)

        yandexDiskStorage.add(key, byteArray)
        val keys = yandexDiskStorage.keys(namespace)
        assertTrue(keys.contains(key))
        assertFailsWith(QBitException::class){
            yandexDiskStorage.add(key, byteArray)
        }
    }

    @InternalCoroutinesApi
    @Test
    fun overwrite() {
        val yandexDiskStorage = YandexDiskStorage(TEST_ACCESS_TOKEN, YandexDiskConfig())
        val namespace = Namespace("production-test")
        val key = Key(namespace, "production-test-file1")
        val byteArray = ByteArray(1234)

        yandexDiskStorage.overwrite(key, byteArray)
        val keys = yandexDiskStorage.keys(namespace)
        assertTrue(keys.contains(key))
    }

    @InternalCoroutinesApi
    @Test
    fun overwriteWithNotExistedFolderStructure() {
        val yandexDiskStorage = YandexDiskStorage(TEST_ACCESS_TOKEN, YandexDiskConfig())
        val namespace = Namespace("production-test1")
        val key = Key(namespace, "production-test-file1")
        val byteArray = ByteArray(1234)

        assertFailsWith(QBitException::class) {
            yandexDiskStorage.overwrite(key, byteArray)
        }
    }

}