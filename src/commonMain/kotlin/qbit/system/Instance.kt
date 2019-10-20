package qbit.system

import qbit.model.Gid

data class Instance(val id: Gid, val iid: Int, val forks: Int, val nextEid: Int)