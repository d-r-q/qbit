@file:Suppress("UNCHECKED_CAST")

package qbit.mapping

import qbit.*
import qbit.model.*
import java.lang.reflect.ParameterizedType
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType


abstract class TypedEntity(var entity: MutableEntitiable) : MutableEntitiable, MaybeEntity {

    override val eid: EID? = (entity as? Entity)?.eid

    override val keys: Set<Attr<Any>>
        get() = entity.keys

    override fun <T : Any> get(key: Attr<T>): T {
        return entity[key]
    }

    override fun <T : Any> tryGet(key: Attr<T>): T? {
        return entity.tryGet(key)
    }

    override val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = entity.entries

    override fun <T : Any> with(key: Attr<T>, value: T): MutableEntitiable {
        entity = entity.with(key, value)
        return entity
    }

    fun with(key: ScalarRefAttr, value: Entity): MutableEntitiable {
        entity = entity.with(key, value)
        return entity
    }

    fun with(key: RefListAttr, value: List<Entity>): MutableEntitiable {
        entity = entity.with(key, value)
        return entity
    }

    override fun with(vararg values: AttrValue<Attr<*>, *>): MutableEntitiable {
        entity = entity.with(*values)
        return entity
    }

    override fun <T : Any> remove(key: Attr<T>): MutableEntitiable {
        entity = entity.remove(key)
        return entity
    }

}

class AttrDelegate<T>(private val attr: Attr<Any>) : ReadWriteProperty<TypedEntity, T> {

    override fun getValue(thisRef: TypedEntity, property: KProperty<*>): T {
        return thisRef.entity.tryGet(attr) as T
    }

    override fun setValue(thisRef: TypedEntity, property: KProperty<*>, value: T) {
        if (value != null) {
            val newEntity = thisRef.entity.with(attr, value as Any)
            thisRef.entity = newEntity
        } else {
            thisRef.entity = thisRef.entity.remove(attr)
        }
    }

}

class ListAttrDelegate<out E : Any, T, L : List<T>?>(private val attr: ListAttr<E>) : ReadWriteProperty<TypedEntity, L> {

    override fun getValue(thisRef: TypedEntity, property: KProperty<*>): L {
        return thisRef.entity.tryGet(attr) as L
    }

    override fun setValue(thisRef: TypedEntity, property: KProperty<*>, value: L) {
        if (value != null) {
            thisRef.entity = thisRef.entity.with(attr, value as List<T>)
        } else {
            thisRef.entity = thisRef.entity.remove(attr)
        }
    }

}

class RefListAttrDelegate<T : TypedEntity, L : List<T>?>(private val attr: RefListAttr) : ReadWriteProperty<TypedEntity, L> {

    override fun getValue(thisRef: TypedEntity, property: KProperty<*>): L {
        val klass = (property.returnType.javaType as ParameterizedType).actualTypeArguments[0]
        val value: List<Entitiable>? = thisRef.entity.tryGet(attr)
        val lst = value?.map {
            when (it) {
                is TypedEntity -> it as T
                else -> typify(it, klass as Class<*>) as T
            }
        } as L
        if (lst != null) {
            thisRef.entity = thisRef.entity.with(attr eq lst as List<Entity>)
        }
        return lst
    }

    override fun setValue(thisRef: TypedEntity, property: KProperty<*>, value: L) {
        if (value != null) {
            thisRef.entity = thisRef.entity.with(attr, value as List<T>)
        } else {
            thisRef.entity = thisRef.entity.remove(attr)
        }
    }

}

class RefAttrDelegate<T : TypedEntity?>(private val attr: ScalarRefAttr) : ReadWriteProperty<TypedEntity, T> {

    override fun getValue(thisRef: TypedEntity, property: KProperty<*>): T {
        val klass = property.returnType.javaType
        return thisRef.entity.tryGet(attr)?.let {
            val typed = typify<TypedEntity>(it, klass as Class<*>)
            thisRef.entity = thisRef.entity.with(attr, typed as Entitiable)
            typed
        } as T
    }

    override fun setValue(thisRef: TypedEntity, property: KProperty<*>, value: T) {
        if (value != null) {
            thisRef.entity = thisRef.entity.with(attr, value as Entitiable)
        } else {
            thisRef.entity = thisRef.entity.remove(attr)
        }
    }

}

inline fun <reified T : TypedEntity> typify(entity: Entitiable): T {
    return typify(entity, T::class.java)
}

fun <T : TypedEntity> typify(entity: Entitiable, klass: Class<*>): T {
    if (entity.javaClass.isAssignableFrom(klass)) {
        return entity as T
    }
    return klass.kotlin.primaryConstructor!!.call(entity) as T
}

inline fun <reified T : TypedEntity> Db.pullAs(eid: EID): T? =
        this.pull(eid)?.let { typify<T>(it) }

inline fun <reified T : TypedEntity> Db.queryAs(vararg preds: QueryPred): Sequence<T> = this.query(*preds).map { typify<T>(it) }

inline fun <reified T : TypedEntity> WriteResult.storedEntityAs() =
        typify<T>(this.storedEntity())

inline fun <reified T : TypedEntity> Entity.typed(): T = typify(this, T::class.java)
