package qbit.api.system

import qbit.api.gid.Gid

data class Instance(val id: Gid, val iid: Int, val forks: Int, val nextEid: Int)