package qbit

import qbit.mapping.attrName
import qbit.model.*
import qbit.ns.Namespace

val qbitNs = Namespace.of("qbit")

object Attrs {

    val name = Attr2(null, Attr2::class.attrName(Attr2::name), QString.code, unique = true, list = false)
    val type = Attr2(null, Attr2::class.attrName(Attr2::type), QByte.code, unique = true, list = false)
    val unique = Attr2(null, Attr2::class.attrName(Attr2::unique), QBoolean.code, unique = false, list = false)
    val list = Attr2(null, Attr2::class.attrName(Attr2::list), QBoolean.code, unique = false, list = false)

}

object Instances {

    private val qbitInstance = qbitNs("instance")

    val forks: ScalarAttr<Int> = ScalarAttr(qbitInstance["forks"], QInt, false)
    val entitiesCount: ScalarAttr<Int> = ScalarAttr(qbitInstance["entities"], QInt, false)
    val iid: ScalarAttr<Int> = ScalarAttr(qbitInstance["iid"], QInt, true)

}

val tombstone = Attr2(null, qbitNs["tombstone"].toStr(), QBoolean.code, false, false)
