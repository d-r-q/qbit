package qbit.platform

expect class BigDecimal(v: Int) {
    constructor(v: Long)
    fun unscaledValue(): BigInteger
    fun scale(): Int
}

expect class BigInteger(array: ByteArray) {
    fun toByteArray(): ByteArray
}

internal expect fun BigInteger.toBigDecimal(scale: Int = 0): BigDecimal

expect inline operator fun BigDecimal.plus(other: BigDecimal): BigDecimal

expect inline operator fun BigDecimal.minus(other: BigDecimal): BigDecimal