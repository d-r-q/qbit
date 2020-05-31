package qbit.api.model

import qbit.api.gid.Gid
import kotlin.reflect.KClass

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
            get() = arrayOf(QBoolean, QByte, QInt, QLong, QString, QBytes, QGid, QRef)

        fun ofCode(code: Byte): DataType<*>? =
                if (code <= 19) {
                    values.firstOrNull { it.code == code }
                } else {
                    values.map { it.list() }.firstOrNull{ it.code == code }
                }

        fun <T : Any> ofValue(value: T?): DataType<T>? = when (value) {
            is Boolean -> QBoolean as DataType<T>
            is Byte -> QByte as DataType<T>
            is Int -> QInt as DataType<T>
            is Long -> QLong as DataType<T>
            is String -> QString as DataType<T>
            is ByteArray -> QBytes as DataType<T>
            is Gid -> QGid as DataType<T>
            is List<*> -> value.firstOrNull()?.let { ofValue(it)?.list() } as DataType<T>
            else -> QRef as DataType<T>
        }
    }

    fun list(): QList<T> {
        // TODO: make types hierarchy: Type -> List | (Scalar -> (Ref | Value))
        require(!isList()) { "Nested lists is not allowed" }
        return QList(this)
    }

    fun isList(): Boolean = (code.toInt().and(32)) > 0

    fun ref(): Boolean = this == QRef || this is QList<*> && this.itemsType == QRef

    fun value(): Boolean = !ref()

    private fun typeClass(): KClass<*> {
        return when (this) {
            is QBoolean -> Boolean::class
            is QByte -> Byte::class
            is QInt -> Int::class
            is QLong -> Long::class
            is QString -> String::class
            is QBytes -> ByteArray::class
            is QGid -> Gid::class
            is QList<*> -> this.itemsType.typeClass()
            QRef -> Any::class
        }
    }

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

object QString : DataType<String>() {

    override val code = 16.toByte()

}

object QBytes : DataType<ByteArray>() {

    override val code = 17.toByte()

}

object QRef : DataType<Any>() {

    override val code = 18.toByte()

}

object QGid : DataType<Gid>() {

    override val code = 19.toByte()

}

fun isListOfVals(list: List<Any>?) =
        list == null || list.isEmpty() || list.firstOrNull()?.let { DataType.ofValue(it)?.value()  }?: true