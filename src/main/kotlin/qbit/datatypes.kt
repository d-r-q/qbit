package qbit

import java.time.Instant
import java.time.ZonedDateTime
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

@Suppress("UNCHECKED_CAST")
sealed class DataType<T : Any> {

    abstract val kotlinType: KClass<T>
    abstract val code: Byte

    companion object {

        private val values: Array<DataType<*>>
            get() = arrayOf(QBoolean, QByte, QInt, QLong, QString, QBytes, QEID, QInstant, QZonedDateTime)

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
            else -> null
        }
    }

    fun compare(v1: T, v2: T): Int = (v1 as Comparable<T>).compareTo(v2)

    fun list() = QList(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataType<*>

        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        return code.toInt()
    }

}

class QList<I : Any, T : List<I>>(private val itemsType: DataType<I>) : DataType<T>() {

    override val code = (100 + itemsType.code).toByte()

    override val kotlinType: KClass<T> = List::class as KClass<T>

}

object QBoolean : DataType<Boolean>() {

    override val code = 0.toByte()

    override val kotlinType = Boolean::class

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

object QInstant : DataType<Instant>() {

    override val code = 4.toByte()

    override val kotlinType = Instant::class

}

object QZonedDateTime : DataType<ZonedDateTime>() {

    override val code = 10.toByte()

    override val kotlinType = ZonedDateTime::class

}

object QString : DataType<String>() {

    override val code = 31.toByte()

    override val kotlinType = String::class

}

object QBytes : DataType<ByteArray>() {

    override val code = 32.toByte()

    override val kotlinType = ByteArray::class

}

object QEntity : DataType<Entity>() {

    override val code = 50.toByte()

    override val kotlinType = Entity::class

}

object QEID : DataType<EID>() {

    override val code = 51.toByte()

    override val kotlinType = EID::class

}