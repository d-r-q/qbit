package qbit.storage

import qbit.Try

interface Storage {

    fun store(key: String, value: ByteArray): Try<Unit>

    fun load(key: String): Try<ByteArray?>

    fun keys(): Try<Collection<String>>

}