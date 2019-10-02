package qbit

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import qbit.ns.Key
import qbit.ns.Namespace
import qbit.platform.Files
import qbit.storage.FileSystemStorage
import qbit.storage.MemStorage
import qbit.storage.Storage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageTest {

    val TEST_ACCESS_TOKEN = "AgAAAAA4v_tAAADLW-MJ7N5YOU1BvQAUJ3i8_XM";
    val YANDEX_DISK_API_CREATE_FOLDER = "https://cloud-api.yandex.net:443/v1/disk/resources?"
//    val YANDEX_DISK_API_CREATE_FOLDER = "https://cloud-api.yandex.net:443/v1/disk/resources"

    @Test
    fun testMemStorage() {
        testStorage(MemStorage())
    }

    @Test
    fun testFilesStorage() {
        // actually it compiles
        val root = Files.createTempDirectory("qbit").toFile()
        val storage = FileSystemStorage(root)
        testStorage(storage)
    }

    @Test
    fun yandexDiskStorageAdd(){
        val namespace0 = Namespace("namespace0");
        val namespace1 = Namespace(namespace0, "namespace1")
        val key : Key = Key(namespace1, "file1");
        val byteArray = ByteArray(1000);

        var job : Job = Job();
        GlobalScope.launch {
            val client = HttpClient()
            var response: String
            job = launch {
                val paths = generatePutResourcePaths(namespace1);
                val iterator = paths.iterator();
                while(iterator.hasNext()){
                    val path = iterator.next();
                    client.put<String>(YANDEX_DISK_API_CREATE_FOLDER) {
                        parameter("path", path)
                        header("Authorization", "OAuth " + TEST_ACCESS_TOKEN);
                    }
                }
            }
            job.join()
            client.close()
            assertTrue(true)
        }
        while (!job.isCompleted) {}
    }

    @Test
    fun yandexDiskStorage() {
        var job : Job = Job();
        GlobalScope.launch {
            val client = HttpClient()
            var htmlContent = "";
            job = launch {
                htmlContent = client.get<String>("https://en.wikipedia.org/wiki/Main_Page");
            }
            job.join();
            client.close();
            print(htmlContent);
            assertTrue(true);
        }
        while (!job.isCompleted) {}
    }

    private fun generatePutResourcePaths(namespace: Namespace) : List<String> {
        val partsArrayList = arrayListOf<List<String>>()
        val pathsArrayList = arrayListOf<String>()
        var currentNamespace = namespace
        while(currentNamespace.parent != null){
            partsArrayList.add(currentNamespace.parts)
            currentNamespace = currentNamespace.parent!!
        }

        val iterator = partsArrayList.iterator();
        while(iterator.hasNext()) {
            val parts = iterator.next();
            pathsArrayList.add(parts.joinToString("/"));
        }
        return pathsArrayList.reversed();
    }

    private fun testStorage(storage: Storage) {
        val rootBytes = byteArrayOf(0, 0, 0, 0)
        val subBytes = byteArrayOf(1, 1, 1, 1)
        val rootNs = Namespace("test-root")
        val subNs = rootNs.subNs("test-sub")

        storage.add(rootNs["root-data"], rootBytes)
        storage.add(subNs["sub-data"], subBytes)

        assertArrayEquals(rootBytes, storage.load(rootNs["root-data"]))
        assertEquals(setOf(rootNs["root-data"]), storage.keys(rootNs).toSet())

        assertArrayEquals(subBytes, storage.load(subNs["sub-data"]))
        assertEquals(setOf(subNs["sub-data"]), storage.keys(subNs).toSet())
    }

//    @Ignore
//    @Test
//    fun testSwapHead() {
//        val user = Namespace("user")
//        val _id = ScalarAttr(user["val"], QString)
//
//        val root = Files.createTempDirectory("qbit").toFile()
//        val storage = FileSystemStorage(root)
//
//        val conn = qbit(storage)
//        conn.persist(_id)
//
//        val e = Entity(_id eq "1")
//        conn.persist(e)
//
//        val loaded = storage.load(Namespace("refs")["head"])
//        //val hash = conn.db.hash.bytes
//        //assertArrayEquals(loaded, hash)
//    }

//    @Test
//    fun testCopyNsConstructor() {
//        val testNs = ns("nodes")("test")
//        val _id = ScalarAttr(testNs["val"], QString)
//
//        val origin = MemStorage()
//        val conn = qbit(origin)
//        conn.persist(_id)
//
//        val e = Entity(_id eq "1")
//        conn.persist(e)
//
//
//        // actually it compiles
//        val rootFile = Files.createTempDirectory("qbit").toFile()
//        val storage = FileSystemStorage(rootFile, origin)
//        assertEquals(origin.subNamespaces(testNs.parent!!), storage.subNamespaces(testNs.parent!!))
//        assertEquals(storage.subNamespaces(root).sortedBy { it.name }, listOf(ns("nodes"), ns("refs")).sortedBy { it.name })
//    }
}