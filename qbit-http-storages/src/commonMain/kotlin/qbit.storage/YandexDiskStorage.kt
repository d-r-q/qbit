//package qbit.storage
//
//
//import io.ktor.client.HttpClient
//import io.ktor.client.request.get
//import io.ktor.client.request.header
//import io.ktor.client.request.parameter
//import io.ktor.client.request.put
//import io.ktor.client.response.HttpResponse
//import io.ktor.client.response.readBytes
//import io.ktor.content.ByteArrayContent
//import kotlinx.coroutines.*
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonConfiguration
//import qbit.ns.Key
//import qbit.ns.Namespace
//import qbit.serialization.Storage
//import kotlin.coroutines.CoroutineContext
//import kotlin.coroutines.EmptyCoroutineContext
//
//@Serializable
//data class UrlToUploadFile(val operation_id: String, val href: String, val method: String, val templated: Boolean?)
//
//class YandexDiskStorage(private val accessToken: String) : Storage, CoroutineScope {
//
//    override val coroutineContext: CoroutineContext
//        get() = EmptyCoroutineContext + DirectCoroutineDispatcher() + CoroutineExceptionHandler { _, t -> throw t }
//
//    val TEST_ACCESS_TOKEN = "AgAAAAA4v_tAAADLW-MJ7N5YOU1BvQAUJ3i8_XM"
//    val YANDEX_DISK_API_GET_DOWNLOAD_FILE_URL = "https://cloud-api.yandex.net:443/v1/disk/resources/download"
//    val YANDEX_DISK_API_GET_URL_TO_UPLOAD_FILE = "https://cloud-api.yandex.net/v1/disk/resources/upload"
//    val YANDEX_DISK_API_CREATE_FOLDER = "https://cloud-api.yandex.net:443/v1/disk/resources"
//    //my access token: AgAAAAA4v_tAAAXk79S51RcMUk1RqX9J_fYXIas
//
//    @InternalCoroutinesApi
//    override fun add(key: Key, value: ByteArray) = runBlocking {
//        val json = Json(JsonConfiguration.Stable)
//
//        val client = HttpClient()
//        val paths = generatePutResourcePaths(key.ns)
//        createFolderStructure(client, paths)
//        val fileUrlResponse = json.parse(UrlToUploadFile.serializer(),
//                getUrlToUploadFile(client, paths.get(paths.size - 1), key.name))
//        var filePutResponse = uploadFile(client, fileUrlResponse.href, value)
//        client.close()
//    }
//
//    @InternalCoroutinesApi
//    override fun overwrite(key: Key, value: ByteArray) = runBlocking {
//        val namespace0 = Namespace("namespace0");
//        val namespace1 = Namespace(namespace0, "namespace1")
//        val key = Key(namespace1, "file1");
//        val byteArray = ByteArray(3000);
//        val json = Json(JsonConfiguration.Stable)
//
//        val client = HttpClient()
//        val paths = generatePutResourcePaths(key.ns)
//        val fileUrlResponse = json.parse(UrlToUploadFile.serializer(),
//                getUrlToUploadFile(client, paths.get(paths.size - 1), key.name))
//        var filePutResponse = uploadFile(client, fileUrlResponse.href, byteArray)
//        client.close()
//    }
//
//    override fun load(key: Key): ByteArray? {
//        val filePath = getFullPath(key);
//
//        val job = GlobalScope.launch {
//            val client = HttpClient()
//            val response = getUrlToDownload(client, filePath)
//
//            //костыль
//            var index = response.indexOf("https");
//            var indexEnd = response.indexOf("\",\"method")
//            val downloadUrl = response.substring(index, indexEnd);
//            //костыль end
//
//            val byteArray = getFile(client, downloadUrl);
//            client.close();
////            return@launch byteArray;
//        }
//        //Как вернуть?
//        return null;
//    }
//
//    override fun keys(namespace: Namespace): Collection<Key> = runBlocking {
//        var fullPath = getFullPath(namespace)
//
//        val client = HttpClient()
//        val response = client.get<String>("https://cloud-api.yandex.net:443/v1/disk/resources"){
//            parameter("path", fullPath)
//            header("Authorization", "OAuth " + TEST_ACCESS_TOKEN)
//        }
//        val json = Json(JsonConfiguration.Stable)
//        val resource = json.parse(Resource.serializer(), response)
//        val files = getFilesByType(resource, "file")
//        client.close()
//    }
//
//    override fun subNamespaces(namespace: Namespace): Collection<Namespace> {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun hasKey(key: Key): Boolean {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    @InternalCoroutinesApi
//    private fun runBlocking(body: suspend () -> Unit) {
//        val job = launch {
//            body()
//        }
//        while (job.isActive) {
//        }
//        if (job.isCancelled) {
//            job.getCancellationException().let { throw it.cause ?: it }
//        }
//    }
//
//    private suspend fun uploadFile(client: HttpClient, urlToUploadFile: String, byteArray: ByteArray) : String{
//        val response = client.put<String>(urlToUploadFile){
//            body = ByteArrayContent(byteArray)
//        }
//        return response;
//    }
//
//    private suspend fun getUrlToUploadFile(client: HttpClient, filePath: String, fileName: String) : String{
//        val response = client.get<String>(YANDEX_DISK_API_GET_URL_TO_UPLOAD_FILE) {
//            parameter("path", filePath + fileName)
//            parameter("overwrite", true)
//            header("Authorization", "OAuth " + TEST_ACCESS_TOKEN);
//        }
//        return response
//    }
//
//    private suspend fun createFolderStructure(client: HttpClient, paths : List<String>){
//        val iterator = paths.iterator();
//        while(iterator.hasNext()){
//            val path = iterator.next();
//            client.put<String>(YANDEX_DISK_API_CREATE_FOLDER) {
//                parameter("path", path)
//                header("Authorization", "OAuth " + TEST_ACCESS_TOKEN);
//            }
//        }
//    }
//
//    //    Helper method for getting paths from namespace and its parents
//    private fun generatePutResourcePaths(namespace: Namespace) : List<String> {
//        val partsArrayList = arrayListOf<List<String>>()
//        val pathsArrayList = arrayListOf<String>()
//        var currentNamespace = namespace
//        while(currentNamespace.parent != null){
//            partsArrayList.add(currentNamespace.parts)
//            currentNamespace = currentNamespace.parent!!
//        }
//
//        val iterator = partsArrayList.iterator();
//        while(iterator.hasNext()) {
//            val parts = iterator.next();
//            pathsArrayList.add(parts.joinToString("/") + "/");
//        }
//        return pathsArrayList.reversed();
//    }
//
//    private suspend fun getFile(client: HttpClient, downloadUrl: String) : ByteArray{
//        val httpResponse = client.get<HttpResponse>(downloadUrl)
//        val byteArray = httpResponse.readBytes()
//        return byteArray;
//    }
//
//    private suspend fun getUrlToDownload(client: HttpClient, path: String) : String{
//        val response = client.get<String>(YANDEX_DISK_API_GET_DOWNLOAD_FILE_URL) {
//            parameter("path", path)
//            header("Authorization", "OAuth $accessToken");
//        }
//        return response;
//    }
//
//    private fun getFullPath(key: Key) : String {
//        val fileName = key.name;
//        val namespace = key.ns;
//        val parts = namespace.parts;
//        val filePath = parts.joinToString("/")
//        return "$filePath/$fileName";
//    }
//}
//
//class DirectCoroutineDispatcher : CoroutineDispatcher() {
//
//    override fun dispatch(context: CoroutineContext, block: Runnable) {
//        block.run()
//    }
//
//}