package qbit.trx

import kotlinx.atomicfu.atomic
import qbit.api.gid.Gid

internal class GidSequence(private val iid: Int, eid: Int): Iterator<Gid>{
    private val atomicEid = atomic(eid)

    override fun next(): Gid = Gid(iid, atomicEid.incrementAndGet())

    override fun hasNext() = true
}