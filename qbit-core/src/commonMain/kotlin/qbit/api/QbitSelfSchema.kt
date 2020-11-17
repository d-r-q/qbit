package qbit.api

import qbit.api.gid.Gid
import qbit.api.gid.Iid
import qbit.api.model.*
import qbit.api.system.Instance


object Attrs {

    val name = Attr<String>(
        Gid(1, 0),
        "Attr/name",
        QString.code,
        unique = true,
        list = false
    )
    val type = Attr<Byte>(
        Gid(1, 1),
        "Attr/type",
        QByte.code,
        unique = false,
        list = false
    )
    val unique = Attr<Boolean>(
        Gid(1, 2),
        "Attr/unique",
        QBoolean.code,
        unique = false,
        list = false
    )
    val list = Attr<Boolean>(
        Gid(1, 3),
        "Attr/list",
        QBoolean.code,
        unique = false,
        list = false
    )

}

object Instances {

    val forks = Attr<Int>(
        Gid(1, 4),
        "Instance/forks",
        QInt.code,
        unique = false,
        list = false
    )
    val nextEid = Attr<Int>(
        Gid(1, 5),
        "Instance/nextEid",
        QInt.code,
        unique = false,
        list = false
    )
    val iid = Attr<Int>(
        Gid(1, 6),
        "Instance/iid",
        QInt.code,
        unique = true,
        list = false
    )

}

val theInstanceGid = Gid(Iid(1, 4), 8)

val theInstanceEid = theInstanceGid.eid

const val firstInstanceEid = 9

val protoInstance = Instance(Gid(Iid(1, 4), theInstanceEid), 1, 0, firstInstanceEid)

val tombstone = Attr<Boolean>(
    Gid(Iid(1, 4), 7),
    "qbit.api/tombstone",
    QBoolean.code,
    unique = false,
    list = false
)

