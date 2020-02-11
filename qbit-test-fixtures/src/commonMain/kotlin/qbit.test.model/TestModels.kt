package qbit.test.model

import kotlinx.serialization.Serializable

@Serializable
data class TheSimplestEntity(val id: Long?, val scalar: String)

@Serializable
data class EntityWithRef(
    val id: Long? = null,
    val ref: TheSimplestEntity
)

@Serializable
data class EntityWithScalarList(val id: Long?, val scalars: List<Int>)

@Serializable
data class EntityWithRefList(val id: Long?, val refs: List<TheSimplestEntity>)

@Serializable
data class MUser(
    val id: Long? = null,
    val login: String,
    val strs: List<String>,
    val theSimplestEntity: TheSimplestEntity,
    val optTheSimplestEntity: TheSimplestEntity?,
    val theSimplestEntities: List<TheSimplestEntity>
)

