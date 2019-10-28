package qbit.storage

import qbit.serialization.Storage


class YandexDiskStorageTest : StorageTest() {

    val TEST_ACCESS_TOKEN = "AgAAAAA4v_tAAADLW-MJ7N5YOU1BvQAUJ3i8_XM";
    val YANDEX_DISK_API_CREATE_FOLDER = "https://cloud-api.yandex.net:443/v1/disk/resources"
    val YANDEX_DISK_API_GET_FILE_URL = "https://cloud-api.yandex.net/v1/disk/resources/upload"
    val YANDEX_DISK_API_GET_DOWNLOAD_FILE_URL = "https://cloud-api.yandex.net:443/v1/disk/resources/download"
    val FILE_URL_HARDCODED = "https://avatars.mds.yandex.net/get-pdb/216365/eb43844b-51d6-41a0-86c0-0f3c47da5b48/s375"

    override fun storage(): Storage {
        // Actually it compiles
        return YandexDiskStorage("provide me")
    }

}