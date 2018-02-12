package qbit

import qbit.schema.Attr
import kotlin.reflect.KClass


interface Entity : Map<Attr<*>, Any> {

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(keyStr: Attr<T>, type: KClass<T>): T? = get(keyStr)?.let {
        if (type == Byte::class && it is Byte) {
            it as T
        } else if (type == Boolean::class && it is Boolean) {
            it as T
        } else if (type.javaObjectType.isAssignableFrom(it.javaClass)) {
            type.java.cast(it)
        } else {
            throw IllegalArgumentException("Could not cast ${it.javaClass} to $type")
        }
    }

}

interface StoredEntity : Entity {

    val eid: EID

}