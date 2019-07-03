package qbit.model

class EID(val iid: Int, val eid: Int) : Comparable<EID> {

    constructor(eid: Long) : this(eid.shr(32).and(0xFFFFFFFF).toInt(), eid.and(0xFFFFFFFF).toInt())

    constructor(iid: IID, eid: Int) : this(iid.value, eid)

    fun value(): Long = (iid.toLong() shl 32) or eid.toLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this::class != other?.let { it::class }) return false

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

    override fun compareTo(other: EID): Int {
        return if (this.iid == other.iid) {
            this.eid.compareTo(other.eid)
        } else {
            this.iid.compareTo(other.iid)
        }
    }

    override fun toString() = "$iid/$eid"

    fun next(step: Int = 1): EID =
            EID(this.iid, this.eid + step)

    fun nextEids(): Iterator<EID> =
            generateSequence(this) { eid -> eid.next() }
                    .iterator()
}