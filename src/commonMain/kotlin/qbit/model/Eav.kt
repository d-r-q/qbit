package qbit.model

fun Fact(gid: Gid, attr: Attr<*>, value: Any) = Eav(gid, attr.name, value)

data class Eav(val gid: Gid, val attr: String, val value: Any)