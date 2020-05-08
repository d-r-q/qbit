package qbit.api.model.impl

import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.model.Entity
import kotlin.reflect.KProperty1


internal val Any.gid: Gid?
    get() {
        if (this is Entity) {
            return this.gid
        }

        val id = this::class.members
            .filterIsInstance<KProperty1<Any, *>>()
            .firstOrNull { it.name == "id" }
            ?.get(this)
        return when (id) {
            null -> null
            is Long -> Gid(id)
            is Gid -> id
            else -> throw QBitException("Unsupported id type: $id of entity $this")
        }
    }
