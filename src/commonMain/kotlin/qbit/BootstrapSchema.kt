package qbit

import qbit.Attrs.list
import qbit.Attrs.name
import qbit.Attrs.type
import qbit.Attrs.unique
import qbit.Instances.forks
import qbit.Instances.iid
import qbit.Instances.nextEid
import qbit.db.Instance
import qbit.model.*
import qbit.ns.Namespace
import qbit.typing.attrName

val qbitNs = Namespace.of("qbit")

object Attrs {

    val name = Attr<String>(Gid(1, 0), Attr::class.attrName(Attr<String>::name), QString.code, unique = true, list = false)
    val type = Attr<Byte>(Gid(1, 1), Attr::class.attrName(Attr<Byte>::type), QByte.code, unique = false, list = false)
    val unique = Attr<Boolean>(Gid(1, 2), Attr::class.attrName(Attr<Boolean>::unique), QBoolean.code, unique = false, list = false)
    val list = Attr<Boolean>(Gid(1, 3), Attr::class.attrName(Attr<Boolean>::list), QBoolean.code, unique = false, list = false)

}

object Instances {

    val forks = Attr<Int>(Gid(1, 4), Instance::class.attrName(Instance::forks), QInt.code, unique = false, list = false)
    val nextEid = Attr<Int>(Gid(1, 5), Instance::class.attrName(Instance::nextEid), QInt.code, unique = false, list = false)
    val iid = Attr<Int>(Gid(1, 6), Instance::class.attrName(Instance::iid), QInt.code, unique = true, list = false)

}

val tombstone = Attr<Boolean>(Gid(IID(1, 4), 7), qbitNs["tombstone"].toStr(), QBoolean.code, unique = false, list = false)

internal val theInstanceGid = Gid(IID(1, 4), 8)
internal val theInstanceEid = theInstanceGid.eid

internal const val firstInstanceEid = 9

val bootstrapSchema: Map<String, Attr<Any>> = mapOf(
        (name.name to name) as Pair<String, Attr<Any>>,
        (type.name to type) as Pair<String, Attr<Any>>,
        (unique.name to unique) as Pair<String, Attr<Any>>,
        (list.name to list) as Pair<String, Attr<Any>>,
        (forks.name to forks) as Pair<String, Attr<Any>>,
        (nextEid.name to nextEid) as Pair<String, Attr<Any>>,
        (iid.name to iid) as Pair<String, Attr<Any>>,
        (tombstone.name to tombstone) as Pair<String, Attr<Any>>
)
