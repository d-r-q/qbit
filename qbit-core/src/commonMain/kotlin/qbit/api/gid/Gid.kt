package qbit.api.gid

data class Gid(val iid: Int, val eid: Int) : Comparable<Gid> {

    constructor(eid: Long) : this(eid.shr(32).and(0xFFFFFFFF).toInt(), eid.and(0xFFFFFFFF).toInt())

    constructor(iid: Iid, eid: Int) : this(iid.value, eid)

    fun value(): Long = (iid.toLong() shl 32) or eid.toLong()

    override fun compareTo(other: Gid): Int {
        return if (this.iid == other.iid) {
            this.eid.compareTo(other.eid)
        } else {
            this.iid.compareTo(other.iid)
        }
    }

    override fun toString() = "$iid/$eid"

}

fun Gid.nextGids(): Iterator<Gid> =
        generateSequence(this) { eid -> Gid(eid.iid, eid.eid + 1) }
                .iterator()
