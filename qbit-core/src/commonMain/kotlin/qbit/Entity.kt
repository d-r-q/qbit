package qbit

import qbit.api.gid.Gid
import qbit.api.model.Tombstone
import qbit.api.model.impl.QTombstone

fun Tombstone(eid: Gid): Tombstone =
    QTombstone(eid)

fun Tombstone(eid: Long): Tombstone =
    QTombstone(Gid(eid))
