package qbit.storage

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readBytes
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import qbit.ns.Key
import qbit.ns.Namespace

class YandexDiskStorage(private val accessToken: String) : Storage {

    val YANDEX_DISK_API_GET_DOWNLOAD_FILE_URL = "https://cloud-api.yandex.net:443/v1/disk/resources/download"
    //my access token: AgAAAAA4v_tAAAXk79S51RcMUk1RqX9J_fYXIas

    override fun add(key: Key, value: ByteArray) {
    }

    override fun overwrite(key: Key, value: ByteArray) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun load(key: Key): ByteArray? {
        val filePath = getFullPath(key);

        val job = GlobalScope.launch {
            val client = HttpClient()
            val response = getUrlToDownload(client, filePath)

            //костыль
            var index = response.indexOf("https");
            var indexEnd = response.indexOf("\",\"method")
            val downloadUrl = response.substring(index, indexEnd);
            //костыль end

            val byteArray = getFile(client, downloadUrl);
            client.close();
//            return@launch byteArray;
        }
        //Как вернуть?
        return null;
    }

    override fun keys(namespace: Namespace): Collection<Key> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subNamespaces(namespace: Namespace): Collection<Namespace> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasKey(key: Key): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private suspend fun getFile(client: HttpClient, downloadUrl: String) : ByteArray{
        val httpResponse = client.get<HttpResponse>(downloadUrl)
        val byteArray = httpResponse.readBytes()
        return byteArray;
    }

    private suspend fun getUrlToDownload(client: HttpClient, path: String) : String{
        val response = client.get<String>(YANDEX_DISK_API_GET_DOWNLOAD_FILE_URL) {
            parameter("path", path)
            header("Authorization", "OAuth $accessToken");
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
}