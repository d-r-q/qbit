package qbit.test.model

import kotlinx.serialization.Serializable

@Serializable
data class TheSimplestEntity(val id: Long?, val scalar: String)

@Serializable
data class IntEntity(val id: Long?, val int: Int)

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
data class ListOfNullables(val id: Long?, val lst: List<Int?>, val refLst: List<Scientist?>)

@Serializable
data class ListOfNullablesHolder(val id: Long?, val nullables: ListOfNullables)

@Serializable
data class NullableList(val id: Long?, val lst: List<Byte>?, val placeholder: Long)

@Serializable
data class MUser(
    val id: Long? = null,
    val login: String,
    val strs: List<String>,
    val theSimplestEntity: TheSimplestEntity,
    val optTheSimplestEntity: TheSimplestEntity?,
    val theSimplestEntities: List<TheSimplestEntity>
)

@Serializable
data class Country(val id: Long?, val name: String, val population: Int?)

@Serializable
data class Scientist(val id: Long?, val externalId: Int, val name: String, val nicks: List<String>, val country: Country, var reviewer: Scientist? = null) {

    override fun toString(): String {
        return "Scientist($id, $name)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Scientist

        if (id != other.id) return false
        if (externalId != other.externalId) return false
        if (name != other.name) return false
        if (nicks != other.nicks) return false
        if (country != other.country) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + externalId
        result = 31 * result + name.hashCode()
        result = 31 * result + nicks.hashCode()
        result = 31 * result + country.hashCode()
        return result
    }
}

@Serializable
data class ResearchGroup(val id: Long?, val members: List<Scientist>)

