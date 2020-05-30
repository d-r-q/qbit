package qbit.api.gid

import kotlinx.serialization.Serializable

@Serializable
data class Iid(val value: Int, val instanceBits: Byte) {

    fun fork(forkNum: Int): Iid {
        require(forkNum > 0) { "Fork num is not positive $forkNum" }
        require(forkNum <= subIidsMask()) { "Too large fork num $forkNum for instance bits $instanceBits (max value is ${subIidsMask()})" }

        var pos = 0
        while (value and (subIidsMask() shl (pos * instanceBits)) != 0) {
            pos++
        }

        return Iid(value or ((forkNum and subIidsMask()) shl (pos * instanceBits)), instanceBits)
    }

    private fun subIidsMask(): Int {
        var mask = 1
        for (i in 1 until instanceBits) {
            mask = mask.shl(1) or 1
        }
        return mask
    }

}