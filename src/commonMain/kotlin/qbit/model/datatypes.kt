package qbit.model

import qbit.platform.BigDecimal
import qbit.platform.Instant
import qbit.platform.ZonedDateTime

/**
 * Possible:
 * - Double
 * - BigInteger
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

@Suppress("UNCHECKED_CAST")
sealed class DataType<out T : Any> {

    abstract val code: Byte

    companion object {

        private val values: Array<DataType<*>>
            get() = arrayOf(QBoolean, QByte, QInt, QLong, QString, QBytes, QEID, QInstant, QZonedDateTime, QDecimal)

        fun ofCode(code: Byte): DataType<*>? = values.firstOrNull { it.code == code }

        fun <T : Any> of(value: T?): DataType<T>? = when (value) {
            is Boolean -> QBoolean as DataType<T>
            is Byte -> QByte as DataType<T>
            is Int -> QInt as DataType<T>
            is Long -> QLong as DataType<T>
            is String -> QString as DataType<T>
            is ByteArray -> QBytes as DataType<T>
            is EID -> QEID as DataType<T>
            is Instant -> QInstant as DataType<T>
            is ZonedDateTime -> QZonedDateTime as DataType<T>
            is BigDecimal -> QDecimal as DataType<T>
            else -> null
        }
    }

    fun list(): QList<T> {
        // TODO: make types hierarchy: Type -> List | (Scalar -> (Ref | Value))
        require(!isList()) { "Nested lists is not allowed" }
        return QList(this)
    }

    fun isList(): Boolean = (code.toInt().and(32)) > 0

}

data class QList<out I : Any>(val itemsType: DataType<I>) : DataType<List<I>>() {

    override val code = (32 + itemsType.code).toByte()

}

object QBoolean : DataType<Boolean>() {

    override val code = 0.toByte()

}

object QByte : DataType<Byte>() {

    override val code = 1.toByte()

}

object QInt : DataType<Int>() {

    override val code = 2.toByte()

}

object QLong : DataType<Long>() {

    override val code = 3.toByte()

}

object QInstant : DataType<Instant>() {

    override val code = 4.toByte()

}

object QDecimal : DataType<BigDecimal>() {

    override val code = 5.toByte()
}


object QZonedDateTime : DataType<ZonedDateTime>() {

    override val code = 8.toByte()

}

object QString : DataType<String>() {

    override val code = 16.toByte()

}

object QBytes : DataType<ByteArray>() {

    override val code = 17.toByte()

}

object QRef : DataType<RoEntity<*>>() {

    override val code = 18.toByte()

}

object QEID : DataType<EID>() {

    override val code = 19.toByte()

}