package qbit.storage

interface Storage {

    fun store(key: String, value: ByteArray)

    fun load(key: String): ByteArray?

    fun keys(): Collection<String>

}