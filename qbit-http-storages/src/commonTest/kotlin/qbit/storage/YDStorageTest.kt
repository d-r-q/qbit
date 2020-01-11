package qbit.storage

import qbit.api.QBitException
import qbit.ns.Key
import qbit.ns.Namespace
import kotlin.test.*

const val accessToken = "AgAAAAA4v_tAAADLW-MJ7N5YOU1BvQAUJ3i8_XM"

class YandexDiskStorageTest {

    @Test
    fun add() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val namespace = Namespace("production-test")
        val key = Key(namespace, "production-test-file1")
        val byteArray = ByteArray(1234)
        if (yandexDiskStorage.hasKey(key)) {
            assertFailsWith(QBitException::class) {
                yandexDiskStorage.add(key, byteArray)
            }
        } else {
            yandexDiskStorage.add(key, byteArray)
            val keys = yandexDiskStorage.keys(namespace)
            assertTrue(keys.contains(key))
        }
    }

    @Test
    fun overwrite() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val namespace = Namespace("production-test")
        val key = Key(namespace, "production-test-file2")
        val byteArray = ByteArray(4321)

        yandexDiskStorage.overwrite(key, byteArray)
        val keys = yandexDiskStorage.keys(namespace)
        assertTrue(keys.contains(key))
    }

    @Test
    fun overwriteWithNotExistedFolderStructure() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val namespace = Namespace("production-test1")
        val key = Key(namespace, "production-test-file1")
        val byteArray = ByteArray(1234)

        assertFailsWith(QBitException::class) {
            yandexDiskStorage.overwrite(key, byteArray)
        }
    }

    @Test
    fun load() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val namespace = Namespace("production-test")
        val key = Key(namespace, "production-test-file1")
        val byteArray = yandexDiskStorage.load(key)
        assertEquals(1234, byteArray?.size)
    }

    @Test
    fun loadNotExistedFile() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val namespace = Namespace("production-test")
        val key = Key(namespace, "not existed file")
        assertFailsWith(QBitException::class) {
            yandexDiskStorage.load(key)
        }
    }

    @Test
    fun loadNotExistedDirectory() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val namespace = Namespace("not existed directory")
        val key = Key(namespace, "production-test-file1")
        assertFailsWith(QBitException::class) {
            yandexDiskStorage.load(key)
        }
    }

    @Test
    fun keys() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val namespace = Namespace(null, "production-test")
        val key = Key(namespace, "production-test-file1")
        val keys = yandexDiskStorage.keys(namespace)
        assertTrue(keys.contains(key))
    }

    @Test
    fun keysNotExistedDirectory() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val namespace = Namespace("not existed directory")
        assertFailsWith(QBitException::class) {
            yandexDiskStorage.keys(namespace)
        }
    }

    @Test
    fun subNamespaces() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val root = Namespace(null, "")
        val subNs = yandexDiskStorage.subNamespaces(root)
        val ns1 = Namespace("namespace0")
        val ns2 = Namespace("nodes")
        val ns3 = Namespace("production-test")
        val ns4 = Namespace("refs")
        val ns5 = Namespace("test-root")
        val ns6 = Namespace("тест")
        assertTrue(subNs.contains(ns1))
        assertTrue(subNs.contains(ns2))
        assertTrue(subNs.contains(ns3))
        assertTrue(subNs.contains(ns4))
        assertTrue(subNs.contains(ns5))
        assertTrue(subNs.contains(ns6))
    }

    @Test
    fun subNamespacesEmptyDirectory() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val ns1 = Namespace("production-test")
        val ns2 = Namespace(ns1, "Новая папка")
        val subNs = yandexDiskStorage.subNamespaces(ns2)
        assertEquals(0, subNs.size)
    }

    @Test
    fun subNamespacesNotExistedDirectory() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val ns = Namespace("not existed directory")
        assertFailsWith(QBitException::class) {
            yandexDiskStorage.subNamespaces(ns)
        }
    }

    @Test
    fun hasKeyDirectory() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val root = Namespace(null, "")
        val key = Key(root, "namespace0")
        val hasKey = yandexDiskStorage.hasKey(key)
        assertTrue(hasKey)
    }

    @Test
    fun hasKeyFile() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val root = Namespace(null, "")
        val key = Key(root, "Москва.jpg")
        val hasKey = yandexDiskStorage.hasKey(key)
        assertTrue(hasKey)
    }

    @Test
    fun hasKeyNotExisted() {
        val yandexDiskStorage = YandexDiskStorage("qbit-yts-test", accessToken)
        val root = Namespace(null, "")
        val key = Key(root, "not existed file")
        val hasKey = yandexDiskStorage.hasKey(key)
        assertFalse(hasKey)
    }
}