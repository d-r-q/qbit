package qbit

import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.AttrValue
import qbit.api.model.Entity
import qbit.api.model.StoredEntity
import qbit.api.model.Tombstone
import qbit.api.model.entity2gid
import qbit.model.DetachedEntity
import qbit.model.QStoredEntity
import qbit.model.QTombstone
import qbit.model.gid

fun Entity(gid: Gid, vararg entries: Any): Entity {
    return DetachedEntity(gid, entries.filterIsInstance<AttrValue<Attr<Any>, Any>>().map { it.attr to entity2gid(it.value) }.toMap())
}

fun AttachedEntity(gid: Gid, entries: Map<Attr<Any>, Any>, resolveGid: (Gid) -> StoredEntity?): StoredEntity {
    return QStoredEntity(gid, entries.toMap().mapValues { if (it.value is Entity) (it.value as Entity).gid else it.value }, resolveGid)
}

fun Tombstone(eid: Gid): Tombstone = QTombstone(eid)

val Any.tombstone
    get() = Tombstone(this.gid!!)