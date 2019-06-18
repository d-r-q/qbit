package qbit.mapping

import qbit.*
import qbit.schema.*
import java.lang.reflect.ParameterizedType
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType


abstract class TypedEntity<E : EID?>(var entity: Entity<E>) : Entity<E> {

    override val eid: E = entity.eid

    override val keys: Set<Attr<Any>>
        get() = entity.keys

    override fun <T : Any> get(key: Attr<T>): T {
        return entity[key]
    }

    override fun <T : Any> getO(key: Attr<T>): T? {
        return entity.getO(key)
    }

    override val entries: Set<AttrValue<Attr<Any>, Any>>
        get() = entity.entries

    override fun <T : Any> with(key: Attr<T>, value: T): Entity<E> {
        entity = entity.with(key, value)
        return entity
    }

    override fun with(key: ScalarRefAttr, value: Entity<*>): Entity<E> {
        entity = entity.with(key, value)
        return entity
    }

    override fun with(key: RefListAttr, value: List<Entity<*>>): Entity<E> {
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

    override fun toIdentified(eid: EID): Entity<EID> {
        return entity.toIdentified(eid)
    }

    override fun detouch(): Entity<E> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class AttrDelegate<T>(private val attr: Attr<Any>) : ReadWriteProperty<TypedEntity<*>, T> {

    override fun getValue(thisRef: TypedEntity<*>, property: KProperty<*>): T {
        return thisRef.entity.getO(attr) as T
    }

    override fun setValue(thisRef: TypedEntity<*>, property: KProperty<*>, value: T) {
        if (value != null) {
            val set: Entity<Nothing> = thisRef.entity.with(attr, value as Any) as Entity<Nothing>
            thisRef.entity = set
        } else {
            thisRef.entity = thisRef.entity.remove(attr) as Entity<Nothing>
        }
    }

}

class ListAttrDelegate<out E : Any, T, L : List<T>?>(private val attr: ListAttr<E>) : ReadWriteProperty<TypedEntity<*>, L> {

    override fun getValue(thisRef: TypedEntity<*>, property: KProperty<*>): L {
        return thisRef.entity.getO(attr) as L
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
        val klass = (property.returnType.javaType as ParameterizedType).actualTypeArguments[0]
        val value: List<Entity<*>>? = thisRef.entity.getO(attr)
        val lst = value?.map {
            when (it) {
                is TypedEntity -> it as T
                else -> typify(it, (klass as ParameterizedType).rawType as Class<*>) as T
            }
        } as L
        if (lst != null) {
            thisRef.entity = thisRef.entity.with(attr eq lst as List<Entity<*>>) as Entity<Nothing>
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
        val klass = property.returnType.javaType
        return thisRef.entity.getO(attr)?.let {
            val typed = typify(it, (klass as ParameterizedType).rawType as Class<*>) as T
            thisRef.entity = thisRef.entity.with(attr, typed as Entity<*>) as Entity<Nothing>
            typed
        } as T
    }

    override fun setValue(thisRef: TypedEntity<*>, property: KProperty<*>, value: T) {
        if (value != null) {
            thisRef.entity = thisRef.entity.with(attr, value as Entity<*>) as Entity<Nothing>
        } else {
            thisRef.entity = thisRef.entity.remove(attr) as Entity<Nothing>
        }
    }

}

inline fun <E : EID?, reified T : TypedEntity<E>> typify(entity: Entity<E>): T {
    return typify(entity, T::class.java)
}

fun <E: EID?, T : TypedEntity<E>> typify(entity: Entity<E>, klass: Class<*>): T {
    if (entity.javaClass.isAssignableFrom(klass)) {
        return entity as T
    }
    return klass.kotlin.primaryConstructor!!.call(entity) as T
}

inline fun <reified T : TypedEntity<EID>> Db.pullAs(eid: EID): T? =
        this.pull(eid)?.let { typify(it) }

inline fun <reified T : TypedEntity<EID>> Db.queryAs(vararg preds: QueryPred): Sequence<T> = this.query(*preds).map { typify<EID, T>(it) }

inline fun <reified T : TypedEntity<EID>> WriteResult.storedEntityAs() =
        typify<EID, T>(this.storedEntity())

inline fun <E : EID?, reified T : TypedEntity<E>> Entity<E>.typed(): T = typify(this, T::class.java)