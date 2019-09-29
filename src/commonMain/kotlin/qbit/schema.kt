package qbit

import qbit.Attrs.list
import qbit.Attrs.name
import qbit.Attrs.type
import qbit.Attrs.unique
import qbit.Instances.forks
import qbit.Instances.iid
import qbit.Instances.nextEid
import qbit.mapping.attrName
import qbit.model.*
import qbit.ns.Namespace
import qbit.trx.Instance

val qbitNs = Namespace.of("qbit")

object Attrs {

    val name = Attr2<String>(EID(1, 0), Attr2::class.attrName(Attr2<String>::name), QString.code, unique = true, list = false)
    val type = Attr2<Byte>(EID(1, 1), Attr2::class.attrName(Attr2<Byte>::type), QByte.code, unique = true, list = false)
    val unique = Attr2<Boolean>(EID(1, 2), Attr2::class.attrName(Attr2<Boolean>::unique), QBoolean.code, unique = false, list = false)
    val list = Attr2<Boolean>(EID(1, 3), Attr2::class.attrName(Attr2<Boolean>::list), QBoolean.code, unique = false, list = false)

}

object Instances {

    val forks = Attr2<Int>(EID(1, 4), Instance::class.attrName(Instance::forks), QInt.code, unique = false, list = false)
    val nextEid = Attr2<Int>(EID(1, 5), Instance::class.attrName(Instance::nextEid), QInt.code, unique = false, list = false)
    val iid = Attr2<Int>(EID(1, 6), Instance::class.attrName(Instance::iid), QInt.code, unique = true, list = false)

}

val tombstone = Attr2<Boolean>(null, qbitNs["tombstone"].toStr(), QBoolean.code, unique = false, list = false)

val bootstrapSchema = mapOf(name.name to name,
        type.name to type,
        unique.name to unique,
        list.name to list,
        forks.name to forks,
        nextEid.name to nextEid,
        iid.name to iid,
        tombstone.name to tombstone
)
