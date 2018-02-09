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
    abstract val code: Byte

    companion object {

        val values: Array<DataType<*>>
            get() = arrayOf(QByte, QInt, QLong, QString, QBytes, QEID)

        fun ofCode(code: Byte): DataType<out Any>? = values.firstOrNull { it.code == code }

        fun of(value: Any) = when (value) {
            is Byte -> QByte
            is Int -> QInt
            is Long -> QLong
            is String -> QString
            is ByteArray -> QBytes
            is EID -> QEID
            else -> null
        }
    }

}

object QByte : DataType<Byte>() {

    override val code = 1.toByte()

    override val kotlinType = Byte::class

}

object QInt : DataType<Int>() {

    override val code = 2.toByte()

    override val kotlinType = Int::class

}

object QLong : DataType<Long>() {

    override val code = 3.toByte()

    override val kotlinType = Long::class

}

object QString : DataType<String>() {

    override val code = 31.toByte()

    override val kotlinType = String::class

}

object QBytes : DataType<ByteArray>() {

    override val code = 32.toByte()

    override val kotlinType = ByteArray::class

}

object QEID : DataType<EID>() {

    override val code = 51.toByte()

    override val kotlinType = EID::class

}