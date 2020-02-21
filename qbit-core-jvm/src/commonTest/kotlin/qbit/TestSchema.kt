package qbit

import kotlinx.serialization.Serializable
import qbit.api.model.Attr
import qbit.api.db.Conn
import qbit.platform.collections.EmptyIterator
import qbit.factorization.attrName
import qbit.factorization.destruct
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.storage.MemStorage
import qbit.spi.Storage
import qbit.schema.schema
import qbit.test.model.*


fun Scientist.toFacts() =
    destruct(this, schemaMap::get, EmptyIterator)

data class Region(val id: Long?, val name: String, val country: Country)

data class City(val id: Long?, val name: String, val region: Region)

data class Paper(val id: Long?, val name: String, val editor: Scientist?)

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


@Serializable
data class NullableScalar(val id: Long?, var scalar: Byte?, val placeholder: Long)

data class NullableScalarWithoutPlaceholder(val id: Long?, val scalar: Int?)

data class NullableRef(val id: Long?, val ref: IntEntity?, val placeholder: Long)

data class EntityWithoutAttrs(val id: Long?)

val testSchema = schema {
    entity(Scientist::class) {
        uniqueInt(it::externalId)
    }
    entity(Country::class) {
        uniqueString(it::name)
    }
    entity(Region::class)
    entity(Paper::class)
    entity(ResearchGroup::class)
    entity(City::class)
    entity(Bomb::class)
    entity(ListOfNullables::class)
    entity(NullableScalar::class)
    entity(NullableList::class)
    entity(NullableRef::class)
    entity(IntEntity::class)
    entity(EntityWithoutAttrs::class)
    entity(NullableScalarWithoutPlaceholder::class)
}

private val gids = Gid(2, 0).nextGids()
val schemaMap: Map<String, Attr<Any>> = testSchema
        .map { it.name to it.id(gids.next()) }
        .toMap()

object Scientists {

    val extId = schemaMap.getValue(Scientist::class.attrName(Scientist::externalId))
    val name = schemaMap.getValue(Scientist::class.attrName(Scientist::name))
    val nicks = schemaMap.getValue(Scientist::class.attrName(Scientist::nicks))
    val reviewer = schemaMap.getValue(Scientist::class.attrName(Scientist::reviewer))
    val country = schemaMap.getValue(Scientist::class.attrName(Scientist::country))
}

object Countries {

    val name = schemaMap.getValue(Country::class.attrName(Country::name))
    val population = schemaMap.getValue(Country::class.attrName(Country::population))

}

object Regions {

    val name = schemaMap.getValue(Region::class.attrName(Region::name))
    val country = schemaMap.getValue(Region::class.attrName(Region::country))

}

object Cities {

    val name = schemaMap.getValue(City::class.attrName(City::name))
    val region = schemaMap.getValue(City::class.attrName(City::region))

}

object Papers {

    val name = schemaMap.getValue(Paper::class.attrName(Paper::name))

}

object ResearchGroups {

    val members = schemaMap.getValue(ResearchGroup::class.attrName(ResearchGroup::members))

}

object Bombs {

    val bool = schemaMap.getValue(Bomb::class.attrName(Bomb::bool))

}

object NullableScalars {

    val scalar = schemaMap.getValue(NullableScalar::class.attrName(NullableScalar::scalar))
    val placeholder = schemaMap.getValue(NullableScalar::class.attrName(NullableScalar::placeholder))

}

object NullableLists {

    val lst = schemaMap.getValue(NullableList::class.attrName(NullableList::lst))
    val placeholder = schemaMap.getValue(NullableList::class.attrName(NullableList::placeholder))

}

object NullableRefs {

    val ref = schemaMap.getValue(NullableRef::class.attrName(NullableRef::ref))
    val placeholder = schemaMap.getValue(NullableRef::class.attrName(NullableRef::placeholder))

}

object IntEntities {

    val int = schemaMap.getValue(IntEntity::class.attrName(IntEntity::int))

}

val uk = Country(gids.next().value(), "United Kingdom", 63_000_000)
val tw = Country(gids.next().value(), "Taiwan", 23_000_000)
val us = Country(gids.next().value(), "USA", 328_000_000)
val ru = Country(gids.next().value(), "Russia", 146_000_000)
val nsk = Region(gids.next().value(), "Novosibirskaya obl.", ru)

val eCodd = Scientist(gids.next().value(), 1, "Edgar Codd", listOf("mathematician", "tabulator"), uk)
val pChen = Scientist(gids.next().value(), 2, "Peter Chen", listOf("unificator"), tw)
val mStonebreaker = Scientist(gids.next().value(), 3, "Michael Stonebreaker", listOf("The DBMS researcher"), us)
val eBrewer = Scientist(gids.next().value(), 4, "Eric Brewer", listOf("Big Data"), us)

fun setupTestSchema(storage: Storage = MemStorage()): Conn {
    val conn = qbit(storage)
    testSchema.forEach {
        conn.persist(it)
    }
    return conn
}

fun setupTestData(storage: Storage = MemStorage()): Conn {
    return with(setupTestSchema(storage)) {
        listOf(eCodd, pChen, mStonebreaker, eBrewer, uk, tw, us, ru, nsk).forEach {
            persist(it)
        }
        this
    }
}