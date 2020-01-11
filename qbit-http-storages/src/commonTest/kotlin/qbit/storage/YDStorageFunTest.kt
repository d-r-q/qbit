package qbit.storage

import qbit.serialization.Storage


class YDStorageFunTest : StorageTest() {

    override fun storage(): Storage =
            YandexDiskStorage("qbit-yds-test", accessToken)

}