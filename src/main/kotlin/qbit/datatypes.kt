package qbit

import kotlin.reflect.KClass


/**
 * Possible:
 * - Boolean
 * - Double
 * - BigInteger
 * - BigDecimal
 * - Instant
 * - ZonedDateTime
 *
 * Datomic:
 * - UUID
 * - URI
 * - Float
 *
 * Realm:
 * - Byte (-> Long in DB)
 * - Short (-> Long in DB)
 * - Int (-> Long in DB)
 * - Date
 * - List<Ref>
 */

sealed class DataType<T : Any> {

    abstract val kotlinType: KClass<T>

    companion object {

        fun of(value: Any) = when (value) {
            is Long -> QLong
            is String -> QString
            is ByteArray -> QBytes
            is EID -> QEID
            else -> throw IllegalArgumentException("Unsupported value type: $value")
        }
    }

}

object QLong : DataType<Long>() {

    override val kotlinType = Long::class

}

object QString : DataType<String>() {

    override val kotlinType = String::class

}

object QBytes : DataType<ByteArray>() {

    override val kotlinType = ByteArray::class

}

object QEID : DataType<EID>() {

    override val kotlinType = EID::class

}