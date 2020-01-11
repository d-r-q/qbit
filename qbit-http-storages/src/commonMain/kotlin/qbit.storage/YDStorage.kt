package qbit.storage

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import qbit.api.QBitException
import qbit.storage.api.Path
import qbit.storage.spi.Storage


class YDStorage(private val config: YDConfig) : Storage {

    private val client = HttpClient()

    override suspend fun exists(path: Path): Boolean {
        val fullPath = path.toYdPath()

        return try {
            client.get<String>(config.ydBaseUrl) {
                parameter("path", fullPath)
                header("Authorization", "OAuth ${config.accessToken}")
            }

            true
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                false
            } else {
                throw QBitException("Could not check path existence", e)
            }
        }
    }

    override fun close() {
        client.close()
    }

    private fun Path.toYdPath() = this@YDStorage.config.storageBasePath
            .resolve(this).els
            .joinToString("/")

}

class YDConfig(internal val ydBaseUrl: String = "", internal val storageBasePath: Path, internal val accessToken: String)

