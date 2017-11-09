package qbit

class EID(val iid: Int, val eid: Int) {

    fun value() = (iid shl 32) or eid

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