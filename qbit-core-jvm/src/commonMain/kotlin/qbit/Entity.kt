package qbit

import qbit.api.gid.Gid
import qbit.api.model.Tombstone
import qbit.api.model.impl.QTombstone
import qbit.api.model.impl.gid

fun Tombstone(eid: Gid): Tombstone = QTombstone(eid)

val Any.tombstone
    get() = Tombstone(this.gid!!)