package qbit.api.system

import kotlinx.serialization.Serializable
import qbit.api.gid.Gid

@Serializable
data class Instance(val id: Gid, val iid: Int, val forks: Int, val nextEid: Int)