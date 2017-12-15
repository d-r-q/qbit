package qbit.storage

import qbit.Try
import qbit.ok

class MemStorage : Storage {
    private val data = HashMap<String, ByteArray>()

    override fun store(key: String, value: ByteArray): Try<Unit> {
        data[key] = value
        return ok(Unit)
    }

    override fun load(key: String): Try<ByteArray?> = ok(data[key])

    override fun keys(): Try<Collection<String>> = ok(data.keys)

}