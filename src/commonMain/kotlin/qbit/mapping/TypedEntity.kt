@file:Suppress("UNCHECKED_CAST")

package qbit.mapping

import qbit.*
import qbit.model.*
import qbit.platform.getRawType
import qbit.platform.getRawTypeOfActualTypeArgument
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


abstract class TypedEntity<E : EID?>(var entity: Entity<E>) : Entity<E> {

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

    override fun <T : Any> with(key: Attr<T>, value: T): Entity<E> {
        entity = entity.with(key, value)
        return entity
    }

    fun with(key: ScalarRefAttr, value: RoEntity<*>): Entity<E> {
        entity = entity.with(key, value)
        return entity
    }

    fun with(key: RefListAttr, value: List<RoEntity<*>>): Entity<E> {
        entity = entity.with(key, value)
        return entity
    }

    override fun with(vararg values: AttrValue<Attr<*>, *>): Entity<E> {
        entity = entity.with(*values)
        return entity
    }

    override fun <T : Any> remove(key: Attr<T>): Entity<E> {
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
            thisRef.entity = newEntity as Entity<Nothing>
        } else {
            thisRef.entity = thisRef.entity.remove(attr) as Entity<Nothing>
        }
    }

}

class ListAttrDelegate<out E : Any, T, L : List<T>?>(private val attr: ListAttr<E>) : ReadWriteProperty<TypedEntity<*>, L> {

    override fun getValue(thisRef: TypedEntity<*>, property: KProperty<*>): L {
        return thisRef.entity.tryGet(attr) as L
    }

    override fun setValue(thisRef: TypedEntity<*>, property: KProperty<*>, value: L) {
        if (value != null) {
            thisRef.entity = thisRef.entity.with(attr, value as List<T>) as Entity<Nothing>
        } else {
            thisRef.entity = thisRef.entity.remove(attr) as Entity<Nothing>
        }
    }

}

class RefListAttrDelegate<T : TypedEntity<*>, L : List<T>?>(private val attr: RefListAttr) : ReadWriteProperty<TypedEntity<*>, L> {

    override fun getValue(thisRef: TypedEntity<*>, property: KProperty<*>): L {
        val klass = getRawTypeOfActualTypeArgument(property)
        val value: List<RoEntity<*>>? = thisRef.entity.tryGet(attr)
        val lst = value?.map {
            when (it) {
                is TypedEntity -> it as T
                else -> typify(it, klass) as T
            }
        } as L
        if (lst != null) {
            thisRef.entity = thisRef.entity.with(attr eq lst as List<QRoEntity<*>>) as Entity<Nothing>
        }
        return lst
    }

    override fun setValue(thisRef: TypedEntity<*>, property: KProperty<*>, value: L) {
        if (value != null) {
            thisRef.entity = thisRef.entity.with(attr, value as List<T>) as Entity<Nothing>
        } else {
            thisRef.entity = thisRef.entity.remove(attr) as Entity<Nothing>
        }
    }

}

class RefAttrDelegate<T : TypedEntity<*>?>(private val attr: ScalarRefAttr) : ReadWriteProperty<TypedEntity<*>, T> {

    override fun getValue(thisRef: TypedEntity<*>, property: KProperty<*>): T {
        val klass = getRawType(property)
        return thisRef.entity.tryGet(attr)?.let {
            val typed = typify(it, klass)
            thisRef.entity = thisRef.entity.with(attr, typed as RoEntity<EID?>) as Entity<Nothing>
            typed
        } as T
    }

    override fun setValue(thisRef: TypedEntity<*>, property: KProperty<*>, value: T) {
        if (value != null) {
            thisRef.entity = thisRef.entity.with(attr, value as RoEntity<*>) as Entity<Nothing>
        } else {
            thisRef.entity = thisRef.entity.remove(attr) as Entity<Nothing>
        }
    }

}

inline fun <E : EID?, reified T : TypedEntity<E>> typify(entity: RoEntity<E>): T {
    return typify(entity, T::class)
}

fun <E : EID?, T : TypedEntity<E>> typify(entity: RoEntity<E>, klass: KClass<*>): T {
    if (klass.isInstance(entity)) {
        return entity as T
    }

    val constr = klass.constructors.first {
        val argClass = getRawType((klass.constructors as List).first().parameters[0])
        argClass.isInstance(entity)
    }
    return constr.call(entity) as T
}

inline fun <reified T : TypedEntity<EID>> Db.pullAs(eid: EID): T? =
        this.pull(eid)?.let { typify<EID, T>(it as RoEntity<EID>) }

inline fun <reified T : TypedEntity<EID>> Db.queryAs(vararg preds: QueryPred): Sequence<T> =
        this.query(*preds).map { typify<EID, T>(it as RoEntity<EID>) }

inline fun <reified T : TypedEntity<EID>> WriteResult.storedEntityAs() =
        typify<EID, T>(this.storedEntity() as RoEntity<EID>)

inline fun <reified E : EID?, reified T : TypedEntity<E>> RoEntity<E>.typed(): T =
        typify(this, T::class)

inline fun <reified E : EID?, reified T : TypedEntity<E>> RoEntity<E>.getAs(attr: RefAttr<T>): T =
        typify(this[attr], T::class)

inline fun <reified E : EID?, reified T : TypedEntity<E>> RoEntity<E>.getAs(attr: RefListAttr): List<T> =
        this[attr].map { typify(it, T::class) } as List<T>
