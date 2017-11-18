package qbit

class EID(val iid: Int, val eid: Int) {

    constructor(eid: Long) : this(eid.shl(32).and(0xFF).toInt(), eid.and(0xFF).toInt())

    constructor(iid: IID, eid: Int) : this(iid.value, eid)

    fun value(): Long = (iid.toLong() shl 32) or eid.toLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EID

        if (iid != other.iid) return false
        if (eid != other.eid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iid
        result = 31 * result + eid
        return result
    }

}