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

val scalarRange = 0u..31u
val listRange = 32u..63u
val pnCounterRange = 64u..95u
val registerRange = 96u..127u
val setRange = 128u..159u

@Suppress("UNCHECKED_CAST")
sealed class DataType<out T : Any> {

    abstract val code: Byte

    companion object {

        private val values: Array<DataType<*>>
            get() = arrayOf(QBoolean, QByte, QInt, QLong, QString, QBytes, QGid, QRef)

        fun ofCode(code: Byte): DataType<*>? = when(code.toUByte()) {
            in scalarRange -> values.firstOrNull { it.code == code }
            in listRange -> ofCode((code.toUByte() - listRange.first).toByte())?.list()
            in pnCounterRange -> ofCode((code.toUByte() - pnCounterRange.first).toByte())?.counter()
            in registerRange -> ofCode((code.toUByte() - registerRange.first).toByte())?.register()
            in setRange -> ofCode((code.toUByte() - setRange.first).toByte())?.set()
            else -> null
        }

        fun <T : Any> ofValue(value: T?): DataType<T>? = when (value) { // TODO REFACTOR
            is Boolean -> QBoolean as DataType<T>
            is Byte -> QByte as DataType<T>
            is Int -> QInt as DataType<T>
            is Long -> QLong as DataType<T>
            is String -> QString as DataType<T>
            is ByteArray -> QBytes as DataType<T>
            is Gid -> QGid as DataType<T>
            is List<*> -> value.firstOrNull()?.let { ofValue(it)?.list() } as DataType<T>?
            else -> QRef as DataType<T>
        }
    }

    fun isScalar(): Boolean = code.toUByte() in scalarRange

    fun list(): QList<T> {
        // TODO: make types hierarchy: Type -> List | (Scalar -> (Ref | Value))
        require(this.isScalar()) { "Nested wrappers is not allowed" }
        return QList(this)
    }

    fun isList(): Boolean = code.toUByte() in listRange

    fun counter(): QCounter<T> {
        require(this is QByte || this is QInt || this is QLong) { "Only primitive number values are allowed in counters" }
        return QCounter(this)
    }

    fun isCounter(): Boolean = code.toUByte() in pnCounterRange

    fun register(): QRegister<T> {
        require(this.isScalar()) { "Nested wrappers is not allowed" }
        return QRegister(this)
    }

    fun isRegister(): Boolean = code.toUByte() in registerRange

    fun set(): QSet<T> {
        require(this.isScalar()) { "Nested wrappers is not allowed" }
        return QSet(this)
    }

    fun isSet(): Boolean = code.toUByte() in setRange

    fun ref(): Boolean = this == QRef ||
            this is QList<*> && this.itemsType == QRef ||
            this is QRegister<*> && this.itemsType == QRef ||
            this is QSet<*> && this.itemsType == QRef

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
            is QCounter<*> -> this.primitiveType.typeClass()
            is QRegister<*> -> this.itemsType.typeClass()
            is QSet<*> -> this.itemsType.typeClass()
            QRef -> Any::class
        }
    }
}

data class QList<out I : Any>(val itemsType: DataType<I>) : DataType<List<I>>() {

    override val code = (listRange.first.toByte() + itemsType.code).toByte()

}

data class QCounter<out I : Any>(val primitiveType: DataType<I>) : DataType<I>() {

    override val code = (pnCounterRange.first.toByte() + primitiveType.code).toByte()

}

data class QRegister<out I : Any>(val itemsType: DataType<I>) : DataType<I>() {

    override val code = (registerRange.first.toByte() + itemsType.code).toByte()

}

data class QSet<out I : Any>(val itemsType: DataType<I>) : DataType<Set<I>>() {

    override val code = (setRange.first.toByte() + itemsType.code).toByte()

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
    list == null || list.isEmpty() || list.firstOrNull()?.let { DataType.ofValue(it)?.value() } ?: true // TODO REFACTOR