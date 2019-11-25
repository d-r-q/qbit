package qbit.storage

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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.io.core.use
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonObject
import qbit.api.QBitException
import qbit.ns.Key
import qbit.ns.Namespace
import qbit.serialization.Storage

data class YandexDiskConfig(
    val YANDEX_DISK_API_GET_FILE_URL: String = "https://cloud-api.yandex.net/v1/disk/resources/upload",
    val YANDEX_DISK_API_GET_DOWNLOAD_FILE_URL: String = "https://cloud-api.yandex.net:443/v1/disk/resources/download",
    val YANDEX_DISK_API_RESOURCES: String = "https://cloud-api.yandex.net:443/v1/disk/resources"
)

class YandexDiskStorage(private val accessToken: String, private val yandexDiskConfig: YandexDiskConfig) : Storage {

    @UnstableDefault
    @InternalCoroutinesApi
    override fun add(key: Key, value: ByteArray) = runBlocking {
        val client = HttpClient()
        client.use {
            val paths = generatePutResourcePaths(key.ns)
            try {
                createFolderStructure(client, paths)
            } catch (e: ClientRequestException) {
                println(e)
                throw QBitException(e.message, e)
            }
            val json = Json.indented.parseJson(getUrlToUploadFile(client, paths[paths.size - 1], key.name).readText())
            val urlToUploadFile = (json as JsonObject).getPrimitive("href").content
            uploadFile(client, urlToUploadFile, value)
            Unit
        }
    }

    @UnstableDefault
    @InternalCoroutinesApi
    override fun overwrite(key: Key, value: ByteArray) = runBlocking {
        val client = HttpClient()
        client.use {
            val paths = generatePutResourcePaths(key.ns)
            try {
                val json =
                    Json.indented.parseJson(getUrlToUploadFile(client, paths[paths.size - 1], key.name).readText())
                val urlToUploadFile = (json as JsonObject).getPrimitive("href").content
                uploadFile(client, urlToUploadFile, value)
            } catch (e: ClientRequestException) {
                throw QBitException(e.message, e)
            }
            Unit
        }

    }

    @UnstableDefault
    @InternalCoroutinesApi
    override fun load(key: Key): ByteArray? = runBlocking {
        val filePath = getFullPath(key)
        val client = HttpClient()
        client.use {
            val response = getUrlToDownload(client, filePath)
            val json = Json.indented.parseJson(response.readText())
            val downloadUrl = (json as JsonObject).getPrimitive("href").content
            val byteArray = getFile(client, downloadUrl)
            byteArray
        }
    }

    @InternalCoroutinesApi
    override fun keys(namespace: Namespace): Collection<Key> = runBlocking {
        val response = getResourceInformation(namespace)
        val json = Json(JsonConfiguration.Stable)
        val resource = json.parse(Resource.serializer(), response.readText())
        val fileNames = getFileNamesInResourceByType(resource, "file")
        val keys = wrapFileNamesToKeys(fileNames, namespace)
        keys
    }

    @InternalCoroutinesApi
    override fun subNamespaces(namespace: Namespace): Collection<Namespace> = runBlocking {
        val response = getResourceInformation(namespace)
        val json = Json(JsonConfiguration.Stable)
        val resource = json.parse(Resource.serializer(), response.readText())
        val dirNames = getFileNamesInResourceByType(resource, "dir")
        val namespaces = wrapDirNamesToNamespaces(dirNames, namespace)
        namespaces
    }

    @InternalCoroutinesApi
    override fun hasKey(key: Key): Boolean = runBlocking {
        val fullPath = getFullPath(key)

        val client = HttpClient()
        client.use {
            try {
                client.get<String>(yandexDiskConfig.YANDEX_DISK_API_RESOURCES) {
                    parameter("path", fullPath)
                    header("Authorization", "OAuth $accessToken")
                }
                true
            } catch (e: ClientRequestException) {
                false
            }
        }
    }

    private suspend fun getResourceInformation(namespace: Namespace): HttpResponse {
        val fullPath = getFullPath(namespace)
        val client = HttpClient()
        client.use {
            return client.get(yandexDiskConfig.YANDEX_DISK_API_RESOURCES) {
                parameter("path", fullPath)
                header("Authorization", "OAuth $accessToken")
            }
        }
    }

    private suspend fun uploadFile(client: HttpClient, urlToUploadFile: String, byteArray: ByteArray): HttpResponse {
        return client.put(urlToUploadFile) {
            body = ByteArrayContent(byteArray)
        }
    }

    private suspend fun getUrlToUploadFile(client: HttpClient, filePath: String, fileName: String): HttpResponse {
        return client.get(yandexDiskConfig.YANDEX_DISK_API_GET_FILE_URL) {
            parameter("path", filePath + fileName)
            parameter("overwrite", true)
            header("Authorization", "OAuth $accessToken")
        }
    }

    private suspend fun createFolderStructure(client: HttpClient, paths: List<String>) {
        val iterator = paths.iterator()
        while (iterator.hasNext()) {
            val path = iterator.next()
            client.put<String>(yandexDiskConfig.YANDEX_DISK_API_RESOURCES) {
                parameter("path", path)
                header("Authorization", "OAuth $accessToken")
            }
        }
    }

    private suspend fun getFile(client: HttpClient, downloadUrl: String): ByteArray {
        val httpResponse = client.get<HttpResponse>(downloadUrl)
        return httpResponse.readBytes()
    }

    private suspend fun getUrlToDownload(client: HttpClient, path: String): HttpResponse {
        return client.get(yandexDiskConfig.YANDEX_DISK_API_GET_DOWNLOAD_FILE_URL) {
            parameter("path", path)
            header("Authorization", "OAuth $accessToken")
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
        val parts = namespace.parts
        return parts.joinToString("/") + "/"
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
            pathsArrayList.add(parts.joinToString("/") + "/")
        }
        return pathsArrayList.reversed()
    }

    /**    Helper method for getting full path to key, including key name */
    private fun getFullPath(key: Key): String {
        val fileName = key.name
        val namespace = key.ns
        val parts = namespace.parts
        val filePath = parts.joinToString("/")
        return "$filePath/$fileName"
    }
}

@InternalCoroutinesApi
fun <T> runBlocking(body: suspend () -> T): T {
    var res: T? = null
    val job = GlobalScope.launch {
        res = body()
    }
    @Suppress("ControlFlowWithEmptyBody")
    while (job.isActive) {
    }
    if (job.isCancelled) {
        job.getCancellationException().let { throw it.cause ?: it }
    }
    return res!!
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ResourceList

        if (sort != other.sort) return false
        if (!items.contentEquals(other.items)) return false
        if (limit != other.limit) return false
        if (offset != other.offset) return false
        if (path != other.path) return false
        if (total != other.total) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sort?.hashCode() ?: 0
        result = 31 * result + items.contentHashCode()
        result = 31 * result + (limit ?: 0)
        result = 31 * result + (offset ?: 0)
        result = 31 * result + path.hashCode()
        result = 31 * result + (total ?: 0)
        return result
    }
}

@Serializable
data class Exif(val date_time: String? = null)

@Serializable
data class CommentIds(val private_resource: String? = null, val public_resource: String? = null)
