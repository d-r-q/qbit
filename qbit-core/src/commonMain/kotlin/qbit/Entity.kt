package qbit

import qbit.api.gid.Gid
import qbit.model.Tombstone
import qbit.model.impl.QTombstone
import qbit.model.impl.gid

fun Tombstone(eid: Gid): Tombstone = QTombstone(eid)

val Any.tombstone
    get() = Tombstone(this.gid!!)