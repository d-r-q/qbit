package qbit.typing

import qbit.api.db.Query
import qbit.api.model.StoredEntity
import kotlin.reflect.KClass


expect class Typing<T : Any>(root: StoredEntity, query: Query, type: KClass<T>) {
    val root: StoredEntity
    val query: Query
    val type: KClass<T>

    fun <R : Any> instantiate(e: StoredEntity, type: KClass<R>): R

}