package qbit.api.model

import qbit.api.gid.Gid

data class Attr<out T : Any>(val id: Gid?, val name: String, val type: Byte, val unique: Boolean, val list: Boolean) {

    init {
        check(!(unique and list))
    }

    fun id(id: Gid) = copy(id = id)

}
