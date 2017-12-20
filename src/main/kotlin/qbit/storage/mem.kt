package qbit.storage

class MemStorage : Storage {
    private val data = HashMap<String, ByteArray>()

    override fun store(key: String, value: ByteArray) {
        data[key] = value
    }

    override fun load(key: String): ByteArray? = data[key]

    override fun keys(): Collection<String> = data.keys

}