package qbit.platform

import java.math.MathContext

actual typealias BigDecimal = java.math.BigDecimal

actual typealias BigInteger = java.math.BigInteger

internal actual fun BigInteger.toBigDecimal(scale: Int): BigDecimal {
    return toBigDecimal(scale, MathContext.UNLIMITED)
}

actual operator fun BigDecimal.plus(other: BigDecimal): BigDecimal = this.add(other)

actual operator fun BigDecimal.minus(other: BigDecimal): BigDecimal = this.subtract(other)