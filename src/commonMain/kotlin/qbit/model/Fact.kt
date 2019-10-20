package qbit.model

fun Fact(eid: Gid, attr: Attr<*>, value: Any) = Fact(eid, attr.name, value)

data class Fact(val eid: Gid, val attr: String, val value: Any)