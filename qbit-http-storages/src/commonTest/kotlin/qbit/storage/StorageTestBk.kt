package qbit

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readBytes
import io.ktor.client.response.readText
import io.ktor.content.ByteArrayContent
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonObject
import qbit.ns.Key
import qbit.ns.Namespace
import qbit.serialization.Storage
import qbit.storage.assertArrayEquals
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@InternalCoroutinesApi
class StorageTestBk : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext + DirectCoroutineDispatcher() + CoroutineExceptionHandler { _, t -> throw t }

    private val TEST_ACCESS_TOKEN = "AgAAAAA4v_tAAADLW-MJ7N5YOU1BvQAUJ3i8_XM"
    private val YANDEX_DISK_API_GET_FILE_URL = "https://cloud-api.yandex.net/v1/disk/resources/upload"
    private val YANDEX_DISK_API_GET_DOWNLOAD_FILE_URL = "https://cloud-api.yandex.net:443/v1/disk/resources/download"
    private val YANDEX_DISK_API_RESOURCES = "https://cloud-api.yandex.net:443/v1/disk/resources"

    @Test
    fun yandexDiskStorageHasKey() = runBlocking {
        val namespace0 = Namespace("namespace0")
        val namespace1 = Namespace(namespace0, "namespace1")
        val key: Key = Key(namespace1, "file2")
        var fullPath = getFullPath(key)

        val client = HttpClient()
        try {
            val response = client.get<HttpResponse>(YANDEX_DISK_API_RESOURCES) {
                parameter("path", fullPath)
                header("Authorization", "OAuth $TEST_ACCESS_TOKEN")
            }
            client.close()
            assertEquals(response.status.value, 200)
        } catch (e: ClientRequestException) {
            assertFalse(false)
            client.close()
        }
    }

    @Test
    fun yandexDiskStorageSubNamespaces() = runBlocking {
        val namespace0 = Namespace("namespace0")
        val namespace1 = Namespace(namespace0, "namespace1")
        var fullPath = getFullPath(namespace1)

        val client = HttpClient()
        val response = client.get<HttpResponse>(YANDEX_DISK_API_RESOURCES) {
            parameter("path", fullPath)
            header("Authorization", "OAuth $TEST_ACCESS_TOKEN")
        }
        val json = Json(JsonConfiguration.Stable)
        val resource = json.parse(Resource.serializer(), response.readText())
        val dirNames = getFileNamesInResourceByType(resource, "dir")
        val dirs = wrapDirNamesToNamespaces(dirNames, namespace1)
        client.close()
        assertEquals(response.status.value, 200)
    }

    @Test
    fun yandexDiskStorageKeys() = runBlocking {
        val namespace0 = Namespace("namespace0")
        val namespace1 = Namespace(namespace0, "namespace1")
        var fullPath = getFullPath(namespace1)

        val client = HttpClient()
        val response = client.get<HttpResponse>(YANDEX_DISK_API_RESOURCES) {
            parameter("path", fullPath)
            header("Authorization", "OAuth $TEST_ACCESS_TOKEN")
        }
        val json = Json(JsonConfiguration.Stable)
        val resource = json.parse(Resource.serializer(), response.readText())
        val files = getFileNamesInResourceByType(resource, "file")
        val keys = wrapFileNamesToKeys(files, namespace1)
        client.close()
        assertEquals(response.status.value, 200)

    }

    @UnstableDefault
    @Test
    fun yandexDiskStorageLoad() = runBlocking {
        val namespace0 = Namespace("namespace0")
        val namespace1 = Namespace(namespace0, "namespace1")
        val key: Key = Key(namespace1, "file1")
        val filePath = getFullPath(key)

        val client = HttpClient()
        val response = getUrlToDownload(client, filePath)
        val responseText = response.readText()
        val json = Json.indented.parseJson(responseText)
        val downloadUrl = (json as JsonObject).getPrimitive("href").content
        val index = responseText.indexOf("size=")
        val indexEnd = responseText.indexOf("&hid")
        val expectedSize = responseText.substring(index + 5, indexEnd).toInt()

        val byteArray = getFile(client, downloadUrl)
        val actualSize = byteArray.size
        client.close()
        assertEquals(expectedSize, actualSize)
    }

    @Test
    fun yandexDistStorageOverwrite() = runBlocking {
        val namespace0 = Namespace("namespace0")
        val namespace1 = Namespace(namespace0, "namespace1")
        val key = Key(namespace1, "file1")
        val byteArray = ByteArray(5000)

        val client = HttpClient()
        val paths = generatePutResourcePaths(key.ns)
        val json = Json.indented.parseJson(getUrlToUploadFile(client, paths[paths.size - 1], key.name).readText())
        var urlToUploadFile = (json as JsonObject).getPrimitive("href").content
        var filePutResponse = uploadFile(client, urlToUploadFile, byteArray)
        client.close()
        assertEquals(filePutResponse.status.value, 201)
    }

    @Test
    fun yandexDiskStorageAdd() = runBlocking {
        val namespace0 = Namespace("namespace0")
        val namespace1 = Namespace(namespace0, "namespace1")
        val key = Key(namespace1, "file7")
        val byteArray = ByteArray(5000)

        val client = HttpClient()
        val paths = generatePutResourcePaths(key.ns)
        try {
            createFolderStructure(client, paths)
        } catch (e: ClientRequestException) {
            println(e)
        }
        val json = Json.indented.parseJson(getUrlToUploadFile(client, paths[paths.size - 1], key.name).readText())
        var urlToUploadFile = (json as JsonObject).getPrimitive("href").content
        var filePutResponse = uploadFile(client, urlToUploadFile, byteArray)
        client.close()
        assertEquals(filePutResponse.status.value, 201)
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

    private suspend fun uploadFile(client: HttpClient, urlToUploadFile: String, byteArray: ByteArray): HttpResponse {
        val response = client.put<HttpResponse>(urlToUploadFile) {
            body = ByteArrayContent(byteArray)
        }
        return response
    }

    private suspend fun getUrlToUploadFile(client: HttpClient, filePath: String, fileName: String): HttpResponse {
        return client.get(YANDEX_DISK_API_GET_FILE_URL) {
            parameter("path", filePath + fileName)
            parameter("overwrite", true)
            header("Authorization", "OAuth $TEST_ACCESS_TOKEN")
        }
    }

    private suspend fun createFolderStructure(client: HttpClient, paths: List<String>) {
        val iterator = paths.iterator()
        while (iterator.hasNext()) {
            val path = iterator.next()
            client.put<String>(YANDEX_DISK_API_RESOURCES) {
                parameter("path", path)
                header("Authorization", "OAuth $TEST_ACCESS_TOKEN")
            }
        }
    }

    /**    Helper method for wrapping dirNames to Namespace Collection */
    private fun wrapDirNamesToNamespaces(dirNames: List<String>, namespace: Namespace): Collection<Namespace> {
        return dirNames.map { dirName -> Namespace(namespace, dirName) }
    }

    /**    Helper method for wrapping fileNames to Keys Collection */
    private fun wrapFileNamesToKeys(fileNames: List<String>, namespace: Namespace): Collection<Key> {
        return fileNames.map { filename -> Key(namespace, filename) }
    }

    /**    Helper method for getting file names by type */
    private fun getFileNamesInResourceByType(resource: Resource, type: String): List<String> {
        val items = resource._embedded?.items
        var files: List<String> = emptyList()
        if (items != null) {
            files = items.filter { item -> item.type == type }.map { item -> item.name }
        }
        return files
    }

    /**    Helper method for getting full path from namespace to root */
    private fun getFullPath(namespace: Namespace): String {
        var parts = namespace.parts
        return parts.joinToString("/") + "/";
    }

    /**    Helper method for getting paths from namespace and its parents */
    private fun generatePutResourcePaths(namespace: Namespace): List<String> {
        val partsArrayList = arrayListOf<List<String>>()
        val pathsArrayList = arrayListOf<String>()
        var currentNamespace = namespace
        while (currentNamespace.parent != null) {
            partsArrayList.add(currentNamespace.parts)
            currentNamespace = currentNamespace.parent!!
        }

        val iterator = partsArrayList.iterator()
        while (iterator.hasNext()) {
            val parts = iterator.next()
            pathsArrayList.add(parts.joinToString("/") + "/");
        }
        return pathsArrayList.reversed()
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

    private suspend fun getFile(client: HttpClient, downloadUrl: String): ByteArray {
        val httpResponse = client.get<HttpResponse>(downloadUrl)
        return httpResponse.readBytes()
    }

    private suspend fun getUrlToDownload(client: HttpClient, path: String): HttpResponse {
        return client.get(YANDEX_DISK_API_GET_DOWNLOAD_FILE_URL) {
            parameter("path", path)
            header("Authorization", "OAuth $TEST_ACCESS_TOKEN")
        };
    }

    private fun getFullPath(key: Key): String {
        val fileName = key.name
        val namespace = key.ns
        val parts = namespace.parts
        val filePath = parts.joinToString("/")
        return "$filePath/$fileName"
    }
}

@Serializable
data class Resource(
    val antivirus_status: String? = null, val resource_id: String? = null, val share: ShareInfo? = null,
    val file: String? = null, val size: Int? = null, val photoslice_time: String? = null,
    val _embedded: ResourceList? = null, val exif: Exif? = null, val custom_properties: JsonObject? = null,
    val media_type: String? = null, val preview: String? = null, val type: String,
    val mime_type: String? = null, val revision: Long? = null, val public_url: String? = null, val path: String,
    val md5: String? = null, val public_key: String? = null, val sha256: String? = null, val name: String,
    val created: String, val modified: String, val comment_ids: CommentIds? = null
)

@Serializable
data class ShareInfo(val is_root: Boolean? = null, val is_owned: Boolean? = null, val rights: String)

@Serializable
data class ResourceList(
    val sort: String? = null, val items: Array<Resource>, val limit: Int? = null, val offset: Int? = null,
    val path: String, val total: Int? = null
)

@Serializable
data class Exif(val date_time: String? = null)

@Serializable
data class CommentIds(val private_resource: String? = null, val public_resource: String? = null)

class DirectCoroutineDispatcher : CoroutineDispatcher() {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }

}