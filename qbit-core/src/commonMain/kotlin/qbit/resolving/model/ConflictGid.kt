package qbit.resolving.model

import qbit.api.gid.Gid
import qbit.api.model.Hash
import qbit.serialization.NodeVal

class ConflictGid (val gid: Gid, val node1: NodeVal<Hash>, val node2: NodeVal<Hash>)