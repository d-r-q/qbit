package qbit

import qbit.model.*
import qbit.ns.Namespace

val qbitNs = Namespace.of("qbit")

object Attrs {

    private val qbitAttrs = qbitNs("attr")

    val name: ScalarAttr<String> = ScalarAttr(qbitAttrs["name"], QString, unique = true)
    val type: ScalarAttr<Byte> = ScalarAttr(qbitAttrs["type"], QByte)
    val unique: ScalarAttr<Boolean> = ScalarAttr(qbitAttrs["unique"], QBoolean)
    val list: ScalarAttr<Boolean> = ScalarAttr(qbitAttrs["list"], QBoolean)

}

object Instances {

    private val qbitInstance = qbitNs("instance")

    val forks: ScalarAttr<Int> = ScalarAttr(qbitInstance["forks"], QInt, false)
    val entitiesCount: ScalarAttr<Int> = ScalarAttr(qbitInstance["entities"], QInt, false)
    val iid: ScalarAttr<Int> = ScalarAttr(qbitInstance["iid"], QInt, true)

}

val tombstone = ScalarAttr(qbitNs["tombstone"], QBoolean, false)
