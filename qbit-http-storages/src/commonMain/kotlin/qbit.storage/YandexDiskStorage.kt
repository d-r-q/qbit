package qbit.storage

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readBytes
import io.ktor.client.response.readText
import io.ktor.content.ByteArrayContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.io.readUTF8Line
import kotlinx.io.core.Closeable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonObject
import qbit.api.QBitException
import qbit.ns.Key
import qbit.ns.Namespace
import qbit.platform.runBlocking
import qbit.serialization.Storage

class YandexDiskStorage(
        private val storageBase: String,
        private val accessToken: String,
        private val yandexDiskBaseUrl: String = "https://cloud-api.yandex.net:443/v1/disk/resources"
) : Storage, Closeable {

    private val client = HttpClient()

    override fun add(key: Key, value: ByteArray) = runBlocking {
        val paths = generatePutResourcePaths(key.ns)
        try {
            createFolderStructure(paths, key.ns)
            val response = getUrlToUploadFile(paths[paths.size - 1], key.name, false)
            val json = Json(JsonConfiguration.Stable)
            val resourceUploadLink = json.parse(ResourceUploadLink.serializer(), response.readText())
            uploadFile(resourceUploadLink.href, value)
        } catch (e: ClientRequestException) {
            println(e.response.content.readUTF8Line())
            throw QBitException(e.message, e)
        }
        Unit
    }

    override fun overwrite(key: Key, value: ByteArray) = runBlocking {
        val paths = generatePutResourcePaths(key.ns)
        try {
            val response = getUrlToUploadFile(paths[paths.size - 1], key.name, true)
            val json = Json(JsonConfiguration.Stable)
            val resourceUploadLink = json.parse(ResourceUploadLink.serializer(), response.readText())
            uploadFile(resourceUploadLink.href, value)
        } catch (e: ClientRequestException) {
            throw QBitException(e.message, e)
        }
        Unit

    }

    override fun load(key: Key): ByteArray? = runBlocking {
        val filePath = getFullPath(key)
        try {
            val response = getUrlToDownload(filePath)
            val json = Json(JsonConfiguration.Stable)
            val link = json.parse(Link.serializer(), response.readText())
            val byteArray = getFile(link.href)
            byteArray
        } catch (e: ClientRequestException) {
            throw QBitException(e.message, e)
        }
    }

    override fun keys(namespace: Namespace): Collection<Key> = runBlocking {
        try {
            val response = getResourceInformation(namespace)
            val json = Json(JsonConfiguration.Stable)
            val resource = json.parse(Resource.serializer(), response.readText())
            val fileNames = getFileNamesInResourceByType(resource, "file")
            val keys = wrapFileNamesToKeys(fileNames, namespace)
            keys
        } catch (e: ClientRequestException) {
            throw QBitException(e.message, e)
        }
    }

    override fun subNamespaces(namespace: Namespace): Collection<Namespace> = runBlocking {
        try {
            val response = getResourceInformation(namespace)
            val json = Json(JsonConfiguration.Stable)
            val resource = json.parse(Resource.serializer(), response.readText())
            val dirNames = getFileNamesInResourceByType(resource, "dir")
            wrapDirNamesToNamespaces(dirNames, namespace)
        } catch (e: ClientRequestException) {
            throw QBitException(e.message, e)
        }
    }

    override fun hasKey(key: Key): Boolean = runBlocking {
        val fullPath = getFullPath(key)

        try {
            client.get<String>(yandexDiskBaseUrl) {
                parameter("path", fullPath)
                header("Authorization", "OAuth $accessToken")
            }
            true
        } catch (e: ClientRequestException) {
            false
        }
    }

    fun deleteNamespace(ns: Namespace): Boolean = runBlocking {
        val fullPath = getFullPath(ns)
        try {
            client.delete<String>(yandexDiskBaseUrl) {
                parameter("path", fullPath)
                header("Authorization", "OAuth $accessToken")
            }
            delay(1000)
            true
        } catch (e: ClientRequestException) {
            e.response.status == HttpStatusCode.NotFound
        }
    }

    override fun close() {
        client.close()
    }

    private suspend fun getResourceInformation(namespace: Namespace): HttpResponse {
        val fullPath = getFullPath(namespace)
        return client.get(yandexDiskBaseUrl) {
            parameter("path", fullPath)
            header("Authorization", "OAuth $accessToken")
        }
    }

    private suspend fun uploadFile(urlToUploadFile: String, byteArray: ByteArray): HttpResponse {
        return client.put(urlToUploadFile) {
            body = ByteArrayContent(byteArray)
        }
    }

    private suspend fun getUrlToUploadFile(
            filePath: String,
            fileName: String,
            overwrite: Boolean
    ): HttpResponse {
        return client.get(yandexDiskBaseUrl) {
            parameter("path", filePath + fileName)
            parameter("overwrite", overwrite)
            header("Authorization", "OAuth $accessToken")
            accept(ContentType.Application.Json)
        }
    }

    private suspend fun createFolderStructure(paths: List<String>, ns: Namespace) {
        val directoryKeys = generateDirectoryPathKeys(ns)
        val pathIterator = paths.iterator()
        val directoryKeyIterator = directoryKeys.iterator()
        while (pathIterator.hasNext() && directoryKeyIterator.hasNext()) {
            val path = pathIterator.next()
            val directoryKey = directoryKeyIterator.next()
            if (hasKey(directoryKey)) {
                continue
            }
            client.put<String>(yandexDiskBaseUrl) {
                parameter("path", path)
                header("Authorization", "OAuth $accessToken")
            }
        }
    }

    private suspend fun getFile(downloadUrl: String): ByteArray {
        val httpResponse = client.get<HttpResponse>(downloadUrl)
        return httpResponse.readBytes()
    }

    private suspend fun getUrlToDownload(path: String): HttpResponse {
        return client.get(yandexDiskBaseUrl) {
            parameter("path", path)
            header("Authorization", "OAuth $accessToken")
        }
    }

    /**
     * Converts namespace and its parents to list of keys
     */
    private fun generateDirectoryPathKeys(ns: Namespace): ArrayList<Key> {
        val path = ns.parts
        val directoryKeys = ArrayList<Key>()
        for (i in 1 until path.size) {
            var current = Namespace(null, "")
            for (j in 1 until i) {
                current = Namespace(current, path[j])
            }
            directoryKeys.add(Key(current, path[i]))
        }
        return directoryKeys
    }

    /**
     * Wraps dirNames to Namespace Collection
     */
    private fun wrapDirNamesToNamespaces(dirNames: Collection<String>, namespace: Namespace): Collection<Namespace> {
        return dirNames.map { dirName -> Namespace(namespace, dirName) }
    }

    /**
     * Wraps fileNames to Keys Collection
     */
    private fun wrapFileNamesToKeys(fileNames: Collection<String>, namespace: Namespace): Collection<Key> {
        return fileNames.map { filename -> Key(namespace, filename) }
    }

    /**
     * Finds all files in Resource, matched by type, and returns matched filenames
     */
    private fun getFileNamesInResourceByType(resource: Resource, type: String): Collection<String> {
        return resource._embedded?.items
                ?.filter { item -> item.type == type }
                ?.map { item -> item.name } ?: emptyList()
    }

    /**
     * Converts namespace to list of paths to itself and its parents
     */
    private fun generatePutResourcePaths(namespace: Namespace): List<String> {
        val partsArrayList = arrayListOf<List<String>>()
        var currentNamespace = namespace
        while (currentNamespace.parent != null) {
            partsArrayList.add(currentNamespace.parts)
            currentNamespace = currentNamespace.parent!!
        }
        return listOf(storageBase) + partsArrayList.map { part -> part.joinToString("/") + "/" }.reversed()
    }

    /**
     * Converts key to path to file within YD storage
     */
    private fun getFullPath(key: Key): String {
        val fileName = key.name
        val filePath = getFullPath(key.ns)
        return filePath + fileName
    }

    /**
     * Сonverts namespace to full path from root to itself within YD storage
     */
    private fun getFullPath(namespace: Namespace): String {
        val parts = namespace.parts
        return storageBase + "/" + parts.joinToString("/") + "/"
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
        val sort: String? = null, val items: List<Resource>, val limit: Int? = null, val offset: Int? = null,
        val path: String, val total: Int? = null
)

@Serializable
data class Exif(val date_time: String? = null)

@Serializable
data class CommentIds(val private_resource: String? = null, val public_resource: String? = null)

@Serializable
data class ResourceUploadLink(
        val operation_id: String,
        val href: String,
        val method: String,
        val templated: Boolean? = null
)

@Serializable
data class Link(val href: String, val method: String, val templated: Boolean? = null)