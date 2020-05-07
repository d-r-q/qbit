package qbit.test.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

@Serializable
data class TheSimplestEntity(val id: Long?, val scalar: String)

@Serializable
data class IntEntity(val id: Long?, val int: Int)

@Serializable
data class NullableIntEntity(val id: Long?, val int: Int?)

@Serializable
data class ByteArrayEntity(val id: Long?, val byteArray: ByteArray)

@Serializable
data class ListOfByteArraysEntity(val id: Long?, val byteArrays: List<ByteArray>)

@Serializable
data class EntityWithRef(
    val id: Long? = null,
    val ref: TheSimplestEntity
)

@Serializable
data class EntityWithRefToNullableInt(val id: Long?, val nullableInt: NullableIntEntity)

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
data class NullableScalar(val id: Long?, var scalar: Byte?, val placeholder: Long)

@Serializable
data class ParentToChildrenTreeEntity(val id: Long?, val name: String, val children: List<ParentToChildrenTreeEntity>)

@Serializable
data class EntityWithRefsToSameType(val id: Long?, val ref1: TheSimplestEntity, val ref2: TheSimplestEntity)

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
data class Region(val id: Long?, val name: String, val country: Country)

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

@Serializable
data class Bomb(val id: Long?,

                val bool: Boolean,
                val optBool: Boolean?,
                val boolList: List<Boolean>,
                val boolListOpt: List<Boolean>?,
                var mutBool: Boolean,
                var mutOptBool: Boolean?,
                var mutBoolList: List<Boolean>,
                var mutBoolListOpt: List<Boolean>?,

                val byte: Byte,
                val optByte: Byte?,
                val byteList: List<Byte>,
                val byteListOpt: List<Byte>?,

                val int: Int,
                val optInt: Int?,
                val intList: List<Int>,
                val intListOpt: List<Int>?,

                val long: Long,
                val optLong: Long?,
                val longList: List<Long>,
                val longListOpt: List<Long>?,

                val str: String,
                val optStr: String?,
                val strList: List<String>,
                val strListOpt: List<String>?,

                val bytes: ByteArray,
                val optBytes: ByteArray?,
                val bytesList: List<ByteArray>,
                val bytesListOpt: List<ByteArray>?,

                val country: Country,
                val optCountry: Country?,
                val countiesList: List<Country>,
                val countriesListOpt: List<Country>?,

                var mutCountry: Country,
                var mutOptCountry: Country?,
                var mutCountriesList: List<Country>,
                var mutCountriesListOpt: List<Country>?,

                var optBomb: Bomb?


) {
    override fun toString(): String {
        return "Bomb()"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Bomb

        if (id != other.id) return false
        if (bool != other.bool) return false
        if (optBool != other.optBool) return false
        if (boolList != other.boolList) return false
        if (boolListOpt != other.boolListOpt) return false
        if (mutBool != other.mutBool) return false
        if (mutOptBool != other.mutOptBool) return false
        if (mutBoolList != other.mutBoolList) return false
        if (mutBoolListOpt != other.mutBoolListOpt) return false
        if (byte != other.byte) return false
        if (optByte != other.optByte) return false
        if (byteList != other.byteList) return false
        if (byteListOpt != other.byteListOpt) return false
        if (int != other.int) return false
        if (optInt != other.optInt) return false
        if (intList != other.intList) return false
        if (intListOpt != other.intListOpt) return false
        if (long != other.long) return false
        if (optLong != other.optLong) return false
        if (longList != other.longList) return false
        if (longListOpt != other.longListOpt) return false
        if (str != other.str) return false
        if (optStr != other.optStr) return false
        if (strList != other.strList) return false
        if (strListOpt != other.strListOpt) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (optBytes != null) {
            if (other.optBytes == null) return false
            if (!optBytes.contentEquals(other.optBytes)) return false
        } else if (other.optBytes != null) return false
        if (bytesList != other.bytesList) return false
        if (bytesListOpt != other.bytesListOpt) return false
        if (country != other.country) return false
        if (optCountry != other.optCountry) return false
        if (countiesList != other.countiesList) return false
        if (countriesListOpt != other.countriesListOpt) return false
        if (mutCountry != other.mutCountry) return false
        if (mutOptCountry != other.mutOptCountry) return false
        if (mutCountriesList != other.mutCountriesList) return false
        if (mutCountriesListOpt != other.mutCountriesListOpt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + bool.hashCode()
        result = 31 * result + (optBool?.hashCode() ?: 0)
        result = 31 * result + boolList.hashCode()
        result = 31 * result + (boolListOpt?.hashCode() ?: 0)
        result = 31 * result + mutBool.hashCode()
        result = 31 * result + (mutOptBool?.hashCode() ?: 0)
        result = 31 * result + mutBoolList.hashCode()
        result = 31 * result + (mutBoolListOpt?.hashCode() ?: 0)
        result = 31 * result + byte
        result = 31 * result + (optByte ?: 0)
        result = 31 * result + byteList.hashCode()
        result = 31 * result + (byteListOpt?.hashCode() ?: 0)
        result = 31 * result + int
        result = 31 * result + (optInt ?: 0)
        result = 31 * result + intList.hashCode()
        result = 31 * result + (intListOpt?.hashCode() ?: 0)
        result = 31 * result + long.hashCode()
        result = 31 * result + (optLong?.hashCode() ?: 0)
        result = 31 * result + longList.hashCode()
        result = 31 * result + (longListOpt?.hashCode() ?: 0)
        result = 31 * result + str.hashCode()
        result = 31 * result + (optStr?.hashCode() ?: 0)
        result = 31 * result + strList.hashCode()
        result = 31 * result + (strListOpt?.hashCode() ?: 0)
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + (optBytes?.contentHashCode() ?: 0)
        result = 31 * result + bytesList.hashCode()
        result = 31 * result + (bytesListOpt?.hashCode() ?: 0)
        result = 31 * result + country.hashCode()
        result = 31 * result + (optCountry?.hashCode() ?: 0)
        result = 31 * result + countiesList.hashCode()
        result = 31 * result + (countriesListOpt?.hashCode() ?: 0)
        result = 31 * result + mutCountry.hashCode()
        result = 31 * result + (mutOptCountry?.hashCode() ?: 0)
        result = 31 * result + mutCountriesList.hashCode()
        result = 31 * result + (mutCountriesListOpt?.hashCode() ?: 0)
        return result
    }


}

val testsSerialModule = SerializersModule {
    contextual(TheSimplestEntity::class, TheSimplestEntity.serializer())
    contextual(EntityWithRef::class, EntityWithRef.serializer())
    contextual(EntityWithScalarList::class, EntityWithScalarList.serializer())
    contextual(EntityWithRefList::class, EntityWithRefList.serializer())
    contextual(ListOfNullablesHolder::class, ListOfNullablesHolder.serializer())
    contextual(NullableList::class, NullableList.serializer())
    contextual(MUser::class, MUser.serializer())
    contextual(ResearchGroup::class, ResearchGroup.serializer())
    contextual(Scientist::class, Scientist.serializer())
    contextual(Country::class, Country.serializer())
    contextual(NullableScalar::class, NullableScalar.serializer())
    contextual(Bomb::class, Bomb.serializer())
    contextual(NullableIntEntity::class, NullableIntEntity.serializer())
    contextual(EntityWithRefToNullableInt::class, EntityWithRefToNullableInt.serializer())
    contextual(ByteArrayEntity::class, ByteArrayEntity.serializer())
    contextual(ListOfByteArraysEntity::class, ListOfByteArraysEntity.serializer())
    contextual(IntEntity::class, IntEntity.serializer())
    contextual(Region::class, Region.serializer())
    contextual(ParentToChildrenTreeEntity::class, ParentToChildrenTreeEntity.serializer())
    contextual(EntityWithRefsToSameType::class, EntityWithRefsToSameType.serializer())
}
