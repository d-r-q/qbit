package qbit.typing

import qbit.*
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.*
import qbit.factoring.Factor
import qbit.factoring.types
import qbit.index.Index
import qbit.index.IndexDb
import qbit.query.EagerQuery
import qbit.query.GraphQuery
import qbit.schema.schema
import qbit.test.model.*
import kotlin.test.*


abstract class MappingTest(val factor: Factor) {

    @Test
    fun `Test simple entity mapping`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema(testsSerialModule) {
            entity(MUser::class) {
                uniqueString(MUser::login)
            }
            entity(TheSimplestEntity::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { factor(it, EmptyDb::attr, gids) }))

        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                theSimplestEntity = TheSimplestEntity(null, "addr"),
                optTheSimplestEntity = TheSimplestEntity(null, "optAddr"),
                theSimplestEntities = listOf(TheSimplestEntity(null, "lstAddr"))
        )

        val facts = factor(user, db::attr, gids)
        val db2 = IndexDb(db.index.addFacts(facts))
        val se = db2.pullEntity(facts.entityFacts[user]!!.first().gid)!!

        val eagerTyping = Typing(se, EagerQuery(), MUser::class)
        val fullUser = eagerTyping.instantiate(se, MUser::class)
        assertEquals("optAddr", fullUser.optTheSimplestEntity!!.scalar)
        assertEquals("login", fullUser.login)
        assertEquals(listOf("str1", "str2"), fullUser.strs)
        assertEquals("addr", fullUser.theSimplestEntity.scalar)
        assertEquals("lstAddr", fullUser.theSimplestEntities[0].scalar)
    }

    @Test
    fun `Test entity graph with mulitple entity types mapping`() {

        val gids = Gid(0, 0).nextGids()

        val testSchema = schema(testsSerialModule) {
            entity(MUser::class) {
                uniqueString(MUser::login)
            }
            entity(TheSimplestEntity::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { factor(it, EmptyDb::attr, gids) }))

        val entity = TheSimplestEntity(null, "addr")
        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                theSimplestEntity = entity,
                optTheSimplestEntity = entity,
                theSimplestEntities = listOf(entity)
        )
        val facts = factor(user, db::attr, gids)
        val db2 = IndexDb(db.index.addFacts(facts))

        val se = db2.pullEntity(facts.entityFacts[user]!!.first().gid)!!
        val eagerTyping = Typing(se, EagerQuery(), MUser::class)
        val fullUser = eagerTyping.instantiate(se, MUser::class)

        assertTrue(fullUser.theSimplestEntity == fullUser.optTheSimplestEntity && fullUser.optTheSimplestEntity == fullUser.theSimplestEntities[0])
        assertTrue(fullUser.theSimplestEntity === fullUser.optTheSimplestEntity && fullUser.optTheSimplestEntity === fullUser.theSimplestEntities[0])
    }

    @Ignore
    @Test
    fun `test multiple states of entity in entity graph is prohibited`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema(testsSerialModule) {
            entity(MUser::class)
            entity(TheSimplestEntity::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { factor(it, EmptyDb::attr, gids) }))

        val addr = TheSimplestEntity(1, "addr")
        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                theSimplestEntity = addr,
                optTheSimplestEntity = addr,
                theSimplestEntities = listOf(addr)
        )
        val userWithAddr = user.copy(theSimplestEntity = user.theSimplestEntity.copy(scalar = "newAddr"))

        assertThrows<QBitException> {
            factor(userWithAddr, db::attr, gids)
        }
    }

    @Test
    fun `Type of Any(class) is QRef`() {
        assertEquals(QRef, types[Any::class])
    }

    // Support of self-refefencing entitys is under question now
    @Ignore
    @Test
    fun `Test factoring of self-referencing entity`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema(testsSerialModule) {
            entity(Scientist::class)
            entity(Country::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { factor(it, EmptyDb::attr, gids) }))
        val s = Scientist(null, 1, "s", emptyList(), Country(null, "c", 0), null)
        s.reviewer = s

        val facts = factor(s, db::attr, gids)
        assertEquals(6, facts.size)
        assertEquals(Gid(0, 7), facts.first { it.attr == Scientists.reviewer.name }.value)
    }

    @Test
    fun `Test bomb schema generation`() {
        val attrs = schema(testsSerialModule) {
            entity(Bomb::class)
        }
            .associateBy { it.name }
        assertEquals(QBoolean.code, attrs.getValue(".qbit.test.model.Bomb/bool").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/bool").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue(".qbit.test.model.Bomb/boolList").type, "Expected ${QBoolean.list()}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/boolList").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue(".qbit.test.model.Bomb/boolListOpt").type, "Expected ${QBoolean.list()}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/boolListOpt").type)}")
        assertEquals(QBoolean.code, attrs.getValue(".qbit.test.model.Bomb/bool").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/bool").type)}")
        assertEquals(QBoolean.code, attrs.getValue(".qbit.test.model.Bomb/mutBool").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/mutBool").type)}")
        assertEquals(QBoolean.code, attrs.getValue(".qbit.test.model.Bomb/mutOptBool").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/mutOptBool").type)}")
        assertEquals(QBoolean.code, attrs.getValue(".qbit.test.model.Bomb/optBool").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/optBool").type)}")
        assertEquals(QByte.code, attrs.getValue(".qbit.test.model.Bomb/byte").type, "Expected ${QByte}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/byte").type)}")
        assertEquals(QByte.code, attrs.getValue(".qbit.test.model.Bomb/optByte").type, "Expected ${QByte}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/optByte").type)}")
        assertEquals(QByte.list().code, attrs.getValue(".qbit.test.model.Bomb/byteList").type, "Expected ${QByte.list()}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/byteList").type)}")
        assertEquals(QByte.list().code, attrs.getValue(".qbit.test.model.Bomb/byteListOpt").type, "Expected ${QBytes.list()}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/byteListOpt").type)}")
        assertEquals(QBytes.code, attrs.getValue(".qbit.test.model.Bomb/bytes").type, "Expected ${QBytes}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/bytes").type)}")
        assertEquals(QBytes.code, attrs.getValue(".qbit.test.model.Bomb/optBytes").type, "Expected ${QBytes}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/optBytes").type)}")
        assertEquals(QInt.code, attrs.getValue(".qbit.test.model.Bomb/int").type, "Expected ${QInt}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/int").type)}")
        assertEquals(QInt.code, attrs.getValue(".qbit.test.model.Bomb/optInt").type, "Expected ${QInt}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/optInt").type)}")
        assertEquals(QLong.code, attrs.getValue(".qbit.test.model.Bomb/long").type, "Expected ${QLong}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/long").type)}")
        assertEquals(QLong.code, attrs.getValue(".qbit.test.model.Bomb/optLong").type, "Expected ${QLong}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/optLong").type)}")
        assertEquals(QRef.code, attrs.getValue(".qbit.test.model.Bomb/country").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/country").type)}")
        assertEquals(QRef.code, attrs.getValue(".qbit.test.model.Bomb/mutCountry").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/mutCountry").type)}")
        assertEquals(QRef.code, attrs.getValue(".qbit.test.model.Bomb/mutOptCountry").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/mutOptCountry").type)}")
        assertEquals(QRef.code, attrs.getValue(".qbit.test.model.Bomb/optBomb").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/optBomb").type)}")
        assertEquals(QRef.code, attrs.getValue(".qbit.test.model.Bomb/optCountry").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/optCountry").type)}")
        assertEquals(QString.code, attrs.getValue(".qbit.test.model.Bomb/optStr").type, "Expected ${QString}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/optStr").type)}")
        assertEquals(QString.code, attrs.getValue(".qbit.test.model.Bomb/str").type, "Expected ${QString}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/str").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue(".qbit.test.model.Bomb/boolList").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/boolList").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue(".qbit.test.model.Bomb/boolListOpt").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/boolListOpt").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue(".qbit.test.model.Bomb/mutBoolList").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/mutBoolList").type)}")
        assertEquals(QBoolean.list().code, attrs.getValue(".qbit.test.model.Bomb/mutBoolListOpt").type, "Expected ${QBoolean}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/mutBoolListOpt").type)}")
        assertEquals(QBytes.list().code, attrs.getValue(".qbit.test.model.Bomb/bytesList").type, "Expected ${QBytes}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/bytesList").type)}")
        assertEquals(QBytes.list().code, attrs.getValue(".qbit.test.model.Bomb/bytesListOpt").type, "Expected ${QBytes}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/bytesListOpt").type)}")
        assertEquals(QInt.list().code, attrs.getValue(".qbit.test.model.Bomb/intList").type, "Expected ${QInt}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/intList").type)}")
        assertEquals(QInt.list().code, attrs.getValue(".qbit.test.model.Bomb/intListOpt").type, "Expected ${QInt}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/intListOpt").type)}")
        assertEquals(QLong.list().code, attrs.getValue(".qbit.test.model.Bomb/longList").type, "Expected ${QLong}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/longList").type)}")
        assertEquals(QLong.list().code, attrs.getValue(".qbit.test.model.Bomb/longListOpt").type, "Expected ${QLong}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/longListOpt").type)}")
        assertEquals(QRef.list().code, attrs.getValue(".qbit.test.model.Bomb/countiesList").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/countiesList").type)}")
        assertEquals(QRef.list().code, attrs.getValue(".qbit.test.model.Bomb/countriesListOpt").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/countriesListOpt").type)}")
        assertEquals(QRef.list().code, attrs.getValue(".qbit.test.model.Bomb/mutCountriesList").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/mutCountriesList").type)}")
        assertEquals(QRef.list().code, attrs.getValue(".qbit.test.model.Bomb/mutCountriesListOpt").type, "Expected ${QRef}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/mutCountriesListOpt").type)}")
        assertEquals(QString.list().code, attrs.getValue(".qbit.test.model.Bomb/strList").type, "Expected ${QString}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/strList").type)}")
        assertEquals(QString.list().code, attrs.getValue(".qbit.test.model.Bomb/strListOpt").type, "Expected ${QString}, but got ${DataType.ofCode(attrs.getValue(".qbit.test.model.Bomb/strListOpt").type)}")
    }
}

