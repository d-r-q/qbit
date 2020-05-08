package qbit.factoring

import qbit.api.gid.Gid
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty1

internal actual fun findGidProp(root: Any): KCallable<*> =
    root::class.members
        .firstOrNull {
            it is KProperty1<*, *> && it.name == "id" && (it.returnType.classifier == Long::class || it.returnType.classifier == Gid::class)
        }
        ?: throw IllegalArgumentException("Entity $root does not contains `id: (Long|Gid)` property")