package qbit

import qbit.model.*
import qbit.ns.Namespace

object EAttr {

    private val qbitAttrs = Namespace.of("qbit", "attr")

    val name: ScalarAttr<String> = ScalarAttr(qbitAttrs["name"], QString, unique = true)
    val type: ScalarAttr<Byte> = ScalarAttr(qbitAttrs["type"], QByte)
    val unique: ScalarAttr<Boolean> = ScalarAttr(qbitAttrs["unique"], QBoolean)
    val list: ScalarAttr<Boolean> = ScalarAttr(qbitAttrs["list"], QBoolean)

}

object EInstance {

    private val qbitInstance = Namespace.of("qbit", "instance")

    val forks: ScalarAttr<Int> = ScalarAttr(qbitInstance["forks"], QInt, false)
    val entitiesCount: ScalarAttr<Int> = ScalarAttr(qbitInstance["entities"], QInt, false)
    val iid: ScalarAttr<Int> = ScalarAttr(qbitInstance["iid"], QInt, true)

}

val tombstone = ScalarAttr(Namespace.of("qbit")["tombstone"], QBoolean, false)
