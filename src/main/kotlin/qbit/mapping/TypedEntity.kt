@file:Suppress("UNCHECKED_CAST")

package qbit.mapping

import qbit.*
import qbit.model.*
import java.lang.reflect.ParameterizedType
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType


abstract class TypedEntity<E : EID?>(var entity: MutableEntitiable<E>) : MutableEntitiable<E> {

    override val eid: E
        get() = entity.eid

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

    override fun <T : Any> with(key: Attr<T>, value: T): MutableEntitiable<E> {
        entity = entity.with(key, value)
        return entity
    }

    fun with(key: ScalarRefAttr, value: Entity<*>): MutableEntitiable<E> {
        entity = entity.with(key, value)
        return entity
    }

    fun with(key: RefListAttr, value: List<Entity<*>>): MutableEntitiable<E> {
        entity = entity.with(key, value)
        return entity
    }

    override fun with(vararg values: AttrValue<Attr<*>, *>): MutableEntitiable<E> {
        entity = entity.with(*values)
        return entity
    }

    override fun <T : Any> remove(key: Attr<T>): MutableEntitiable<E> {
        entity = entity.remove(key)
        return entity
    }

}

class AttrDelegate<T>(private val attr: Attr<Any>) : ReadWriteProperty<TypedEntity<*>, T> {

    override fun getValue(thisRef: TypedEntity<*>, property: KProperty<*>): T {
        return thisRef.entity.tryGet(attr) as T
    }

    override fun setValue(thisRef: TypedEntity<*>, property: KProperty<*>, value: T) {
        if (value != null) {
            val newEntity = thisRef.entity.with(attr, value as Any)
            thisRef.entity = newEntity as MutableEntitiable<Nothing>
        } else {
            thisRef.entity = thisRef.entity.remove(attr) as MutableEntitiable<Nothing>
        }
    }

}

class ListAttrDelegate<out E : Any, T, L : List<T>?>(private val attr: ListAttr<E>) : ReadWriteProperty<TypedEntity<*>, L> {

    override fun getValue(thisRef: TypedEntity<*>, property: KProperty<*>): L {
        return thisRef.entity.tryGet(attr) as L
    }

    override fun setValue(thisRef: TypedEntity<*>, property: KProperty<*>, value: L) {
        if (value != null) {
            thisRef.entity = thisRef.entity.with(attr, value as List<T>) as MutableEntitiable<Nothing>
        } else {
            thisRef.entity = thisRef.entity.remove(attr) as MutableEntitiable<Nothing>
        }
    }

}

class RefListAttrDelegate<T : TypedEntity<*>, L : List<T>?>(private val attr: RefListAttr) : ReadWriteProperty<TypedEntity<*>, L> {

    override fun getValue(thisRef: TypedEntity<*>, property: KProperty<*>): L {
        var klass = (property.returnType.javaType as ParameterizedType).actualTypeArguments[0]
        if (klass is ParameterizedType) {
            klass = klass.rawType
        }
        val value: List<Entitiable<*>>? = thisRef.entity.tryGet(attr)
        val lst = value?.map {
            when (it) {
                is TypedEntity -> it as T
                else -> typify(it, klass as Class<*>) as T
            }
        } as L
        if (lst != null) {
            thisRef.entity = thisRef.entity.with(attr eq lst as List<Entity<*>>) as MutableEntitiable<Nothing>
        }
        return lst
    }

    override fun setValue(thisRef: TypedEntity<*>, property: KProperty<*>, value: L) {
        if (value != null) {
            thisRef.entity = thisRef.entity.with(attr, value as List<T>) as MutableEntitiable<Nothing>
        } else {
            thisRef.entity = thisRef.entity.remove(attr) as MutableEntitiable<Nothing>
        }
    }

}

class RefAttrDelegate<T : TypedEntity<*>?>(private val attr: ScalarRefAttr) : ReadWriteProperty<TypedEntity<*>, T> {

    override fun getValue(thisRef: TypedEntity<*>, property: KProperty<*>): T {
        var klass = property.returnType.javaType
        if (klass is ParameterizedType) {
            klass = klass.rawType
        }
        return thisRef.entity.tryGet(attr)?.let {
            val typed = typify(it, klass as Class<*>)
            thisRef.entity = thisRef.entity.with(attr, typed as Entitiable<EID?>) as MutableEntitiable<Nothing>
            typed
        } as T
    }

    override fun setValue(thisRef: TypedEntity<*>, property: KProperty<*>, value: T) {
        if (value != null) {
            thisRef.entity = thisRef.entity.with(attr, value as Entitiable<*>) as MutableEntitiable<Nothing>
        } else {
            thisRef.entity = thisRef.entity.remove(attr) as MutableEntitiable<Nothing>
        }
    }

}

inline fun <E : EID?, reified T : TypedEntity<E>> typify(entity: Entitiable<E>): T {
    return typify(entity, T::class.java)
}

fun <E : EID?, T : TypedEntity<E>> typify(entity: Entitiable<E>, klass: Class<*>): T {
    if (entity.javaClass.isAssignableFrom(klass)) {
        return entity as T
    }
    val constr = klass.kotlin.constructors.first {
        var argClass = (klass.kotlin.constructors as List).first().parameters[0].type.javaType
        if (argClass is ParameterizedType) {
            argClass = argClass.rawType
        }
        (argClass as? Class<*>)?.isAssignableFrom(entity.javaClass) == true
    }
    return constr.call(entity) as T
}

inline fun <reified T : TypedEntity<EID>> Db.pullAs(eid: EID): T? =
        this.pull(eid)?.let { typify<EID, T>(it as Entitiable<EID>) }

inline fun <reified T : TypedEntity<EID>> Db.queryAs(vararg preds: QueryPred): Sequence<T> =
        this.query(*preds).map { typify<EID, T>(it as Entitiable<EID>) }

inline fun <reified T : TypedEntity<EID>> WriteResult.storedEntityAs() =
        typify<EID, T>(this.storedEntity() as Entitiable<EID>)

inline fun <reified E : EID?, reified T : TypedEntity<E>> Entity<E>.typed(): T =
        typify(this, T::class.java)
