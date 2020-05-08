package qbit.api.model

import qbit.api.gid.Gid

fun Eav(gid: Gid, attr: Attr<*>, value: Any) = Eav(gid, attr.name, value)

data class Eav(val gid: Gid, val attr: String, val value: Any)

object EavComparator : Comparator<Eav> {

    override fun compare(a: Eav, b: Eav): Int {
        return compareValuesBy(a, b, Eav::gid, Eav::attr)
    }

}

