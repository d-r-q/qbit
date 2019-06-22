package qbit.model

import qbit.QBitException

data class IID(val value: Int, val instanceBits: Byte) {

    fun fork(forkNum: Int): IID {
        if (forkNum <= 0) {
            throw QBitException("Fork num is not positive $forkNum")
        }

        var mask = 1
        for (i in 1 until instanceBits) {
            mask = mask.shl(1) or 1
        }

        if (forkNum > mask) {
            throw QBitException("Too large fork num $forkNum for instance bits $instanceBits (max value is $mask)")
        }

        var pos = 0
        while (value and (mask shl (pos * instanceBits)) != 0) {
            pos++
        }

        return IID(value or ((forkNum and mask) shl (pos * instanceBits)), instanceBits)
    }

}