package qbit.test.model

import kotlinx.serialization.Serializable

@Serializable
data class Addr(val id: Long?, val addr: String)

@Serializable
data class UserWithAddr(
    val id: Long? = null,
    val addr: Addr
)

@Serializable
data class MUser(
    val id: Long? = null,
    val login: String,
    val strs: List<String>,
    val addr: Addr,
    val optAddr: Addr?,
    val addrs: List<Addr>
)

