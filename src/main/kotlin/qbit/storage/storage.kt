package qbit.storage

import qbit.Key
import qbit.Namespace

interface Storage {

    fun add(key: Key, value: ByteArray)

    fun overwrite(key: Key, value: ByteArray)

    fun load(key: Key): ByteArray?

    fun keys(namespace: Namespace): Collection<Key>

    fun hasKey(key: Key): Boolean

}