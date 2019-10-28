package qbit

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readBytes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import qbit.ns.Key
import qbit.ns.Namespace
import qbit.serialization.Storage
import qbit.storage.assertArrayEquals
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@InternalCoroutinesApi
class StorageTestBk : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext + DirectCoroutineDispatcher() + CoroutineExceptionHandler { _, t -> throw t }

    val TEST_ACCESS_TOKEN = "AgAAAAA4v_tAAADLW-MJ7N5YOU1BvQAUJ3i8_XM";
    val YANDEX_DISK_API_CREATE_FOLDER = "https://cloud-api.yandex.net:443/v1/disk/resources"
    val YANDEX_DISK_API_GET_FILE_URL = "https://cloud-api.yandex.net/v1/disk/resources/upload"
    val YANDEX_DISK_API_GET_DOWNLOAD_FILE_URL = "https://cloud-api.yandex.net:443/v1/disk/resources/download"
    val FILE_URL_HARDCODED = "https://avatars.mds.yandex.net/get-pdb/216365/eb43844b-51d6-41a0-86c0-0f3c47da5b48/s375"

    @Test
    fun yandexDiskStorageHasKey(){
        val job = GlobalScope.launch {
            val client = HttpClient()
            val response = client.get<String>("https://cloud-api.yandex.net:443/v1/disk/resources/files"){
                parameter("path", "/namespace0")
                header("Authorization", "OAuth " + TEST_ACCESS_TOKEN)
            }
            client.close()
            assertTrue(true);
            //осталось сериализовать и проверить, есть ли в объекте файл
        }
        while (!job.isCompleted) {}
    }

    @Test
    fun yandexDiskStorageSubNamespaces(){
        val job = GlobalScope.launch {
            val client = HttpClient()
            val response = client.get<String>("https://cloud-api.yandex.net:443/v1/disk/resources"){
                parameter("path", "/namespace0")
                header("Authorization", "OAuth " + TEST_ACCESS_TOKEN)
            }
            client.close()
            assertTrue(true);
            //осталось сериализовать и вернуть by type: dir
        }
        while (!job.isCompleted) {}
    }

    @Test
    fun yandexDiskStorageKeys(){
        val job = GlobalScope.launch {
            val client = HttpClient()
            val response = client.get<String>("https://cloud-api.yandex.net:443/v1/disk/resources"){
                parameter("path", "/namespace0")
                header("Authorization", "OAuth " + TEST_ACCESS_TOKEN)
            }
            client.close()
            assertTrue(true);
            //осталось сериализовать и вернуть by type: file
        }
        while (!job.isCompleted) {}
    }

    @UnstableDefault
    @Test
    fun yandexDiskStorageLoad() = runBlocking {
        val namespace0 = Namespace("namespace0");
        val namespace1 = Namespace(namespace0, "namespace1")
        val key: Key = Key(namespace1, "sobaka.png");
        val filePath = getFullPath(key);

        val client = HttpClient() {
            install(JsonFeature)
        }
        val response = getUrlToDownload(client, filePath)
        val json = Json.indented.parseJson(response)
        val downloadUrl = (json as JsonObject).getPrimitive("href").content
        val index = response.indexOf("size=")
        val indexEnd = response.indexOf("&hid")
        val expectedSize = response.substring(index + 5, indexEnd).toInt()

        val byteArray = getFile(client, downloadUrl)
        val actualSize = byteArray.size
        client.close()
        assertEquals(expectedSize, actualSize)
    }

    @Test
    fun yandexDiskStorageAdd() = runBlocking {
        val namespace0 = Namespace("namespace0");
        val namespace1 = Namespace(namespace0, "namespace1")
        val key: Key = Key(namespace1, "file1");
        val byteArray = ByteArray(1000);

        val client = HttpClient()
        val paths = generatePutResourcePaths(namespace1)
        createFolderStructure(client, paths)
        val fileUrlResponse = getFileUrl(client)
        var filePutResponse = uploadFile(client, "", "")
        client.close()
        assertTrue(true)
    }

    private fun runBlocking(body: suspend () -> Unit) {
        val job = launch {
            body()
        }
        while (job.isActive) {
        }
        if (job.isCancelled) {
            job.getCancellationException().let { throw it.cause ?: it }
        }
    }


    private suspend fun uploadFile(client: HttpClient, fileUrl: String, filePath: String) : String{
        val response = client.post<String>(YANDEX_DISK_API_GET_FILE_URL) {
            parameter("path", "sobaka.png")
            parameter("url", FILE_URL_HARDCODED);
            header("Authorization", "OAuth " + TEST_ACCESS_TOKEN);
        }
        return response;
    }

    private suspend fun getFileUrl(client: HttpClient) : String{
        val response = client.get<String>(YANDEX_DISK_API_GET_FILE_URL) {
            parameter("path", "sobaka.png")
            parameter("overwrite", true)
            header("Authorization", "OAuth " + TEST_ACCESS_TOKEN);
        }
        return response;
    }

    private suspend fun createFolderStructure(client: HttpClient, paths : List<String>){
        val iterator = paths.iterator();
        while(iterator.hasNext()){
            val path = iterator.next();
            client.put<String>(YANDEX_DISK_API_CREATE_FOLDER) {
                parameter("path", path)
                header("Authorization", "OAuth " + TEST_ACCESS_TOKEN);
            }
        }
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

    private suspend fun getFile(client: HttpClient, downloadUrl: String) : ByteArray{
        val httpResponse = client.get<HttpResponse>(downloadUrl)
        val byteArray = httpResponse.readBytes()
        return byteArray;
    }

    private suspend fun getUrlToDownload(client: HttpClient, path: String): String {
        val response = client.get<String>(YANDEX_DISK_API_GET_DOWNLOAD_FILE_URL) {
            parameter("path", path)
            header("Authorization", "OAuth $TEST_ACCESS_TOKEN");
        }
        return response;
    }

    private fun getFullPath(key: Key) : String {
        val fileName = key.name;
        val namespace = key.ns;
        val parts = namespace.parts;
        val filePath = parts.joinToString("/")
        return "$filePath/$fileName";
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

data class Resource(val public_key: String, val _embedded: ResourceList, val name: String, val created: String, val custom_properties: Map<String, String>,
                    val public_url: String, val origin_path: String, val modified : String, val path: String, val md5: String, val type: String,
                    val mime_type: String, val size: Int)

data class ResourceList(val sort: String, val public_key: String, val items: Array<Resource>, val path: String, val limit: Int, val offset: Int, val total: Int)

class DirectCoroutineDispatcher : CoroutineDispatcher() {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }

}