package qbit.model


fun Fact(eid: EID, attr: Attr<*>, value: Any) = Fact(eid, attr.str(), value, false)

fun Fact(eid: EID, attr: Attr<*>, value: Any, deleted: Boolean) = Fact(eid, attr.str(), value, deleted)

data class Fact(val eid: EID, val attr: String, val value: Any, val deleted: Boolean)
