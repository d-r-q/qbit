package qbit.model

import qbit.QBitException
import kotlin.reflect.KProperty1

class Gid(val iid: Int, val eid: Int) : Comparable<Gid> {

    constructor(eid: Long) : this(eid.shr(32).and(0xFFFFFFFF).toInt(), eid.and(0xFFFFFFFF).toInt())

    constructor(iid: Iid, eid: Int) : this(iid.value, eid)

    fun value(): Long = (iid.toLong() shl 32) or eid.toLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (!(other is Gid)) return false

        if (iid != other.iid) return false
        if (eid != other.eid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iid
        result = 31 * result + eid
        return result
    }

    override fun compareTo(other: Gid): Int {
        return if (this.iid == other.iid) {
            this.eid.compareTo(other.eid)
        } else {
            this.iid.compareTo(other.iid)
        }
    }

    override fun toString() = "$iid/$eid"

    fun next(step: Int = 1): Gid =
            Gid(this.iid, this.eid + step)

    fun nextGids(): Iterator<Gid> =
            generateSequence(this) { eid -> eid.next() }
                    .iterator()
}

val Any.gid: Gid?
    get() {
        if (this is Entity) {
            return this.gid
        }

        val id = this::class.members
                .filterIsInstance<KProperty1<Any, *>>()
                .firstOrNull { it.name == "id" }
                ?.get(this)
        return when (id) {
            null -> null
            is Long -> Gid(id)
            is Gid -> id
            else -> throw QBitException("Unsupported id type: $id of entity $this")
        }
    }