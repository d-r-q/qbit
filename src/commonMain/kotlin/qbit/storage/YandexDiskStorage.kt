package qbit.storage

import qbit.ns.Key
import qbit.ns.Namespace
import qbit.platform.File

class YandexDiskStorage(private val accessToken: String) : Storage {

    //my access token: AgAAAAA4v_tAAAXk79S51RcMUk1RqX9J_fYXIas
    init {
        val client = HttpClient()
    }


    override fun add(key: Key, value: ByteArray) {

    }

    override fun overwrite(key: Key, value: ByteArray) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun load(key: Key): ByteArray? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
}