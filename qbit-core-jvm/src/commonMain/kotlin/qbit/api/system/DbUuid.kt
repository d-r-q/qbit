package qbit.api.system

import kotlinx.serialization.Serializable
import qbit.api.gid.Iid


@Serializable
data class DbUuid(val iid: Iid)