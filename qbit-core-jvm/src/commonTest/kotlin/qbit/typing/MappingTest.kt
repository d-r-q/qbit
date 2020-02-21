package qbit.typing

import qbit.*
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.*
import qbit.factorization.Destruct
import qbit.factorization.findGidProp
import qbit.factorization.types
import qbit.index.Index
import qbit.index.IndexDb
import qbit.query.EagerQuery
import qbit.query.GraphQuery
import qbit.schema.schema
import qbit.test.model.*
import kotlin.test.*


abstract class MappingTest(val destruct: Destruct) {

    @Test
    fun `Test simple entity mapping`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(MUser::class) {
                uniqueString(it::login)
            }
            entity(TheSimplestEntity::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))

        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                theSimplestEntity = TheSimplestEntity(null, "addr"),
                optTheSimplestEntity = TheSimplestEntity(null, "optAddr"),
                theSimplestEntities = listOf(TheSimplestEntity(null, "lstAddr"))
        )

        val facts = destruct(user, db::attr, gids)
        val db2 = IndexDb(db.index.addFacts(facts))
        val se = db2.pullEntity(facts.entityFacts[user]!!.first().gid)
        val lazyTyping = Typing(se!!, GraphQuery(MUser::class, emptyMap()), MUser::class)
        val u = lazyTyping.instantiate(se, MUser::class)
        assertEquals("login", u.login)
        assertEquals(listOf("str1", "str2"), u.strs)
        assertEquals("addr", u.theSimplestEntity.scalar)
        assertNull(u.optTheSimplestEntity)
        assertEquals("lstAddr", u.theSimplestEntities[0].scalar)

        val eagerTyping = Typing(se, EagerQuery(), MUser::class)
        val fullUser = eagerTyping.instantiate(se, MUser::class)
        assertEquals("optAddr", fullUser.optTheSimplestEntity!!.scalar)
    }

    @Test
    fun `Test entity graph with mulitple entity types mapping`() {

        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(MUser::class) {
                uniqueString(it::login)
            }
            entity(TheSimplestEntity::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))

        val entity = TheSimplestEntity(null, "addr")
        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                theSimplestEntity = entity,
                optTheSimplestEntity = entity,
                theSimplestEntities = listOf(entity)
        )
        val facts = destruct(user, db::attr, gids)
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

        val testSchema = schema {
            entity(MUser::class)
            entity(TheSimplestEntity::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))

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
            destruct(userWithAddr, db::attr, gids)
        }
    }

    @Test
    fun `Type of Any(class) is QRef`() {
        assertEquals(QRef, types[Any::class])
    }

    @Test
    fun `findGidProp should return only properties with name 'id'`() {

        @Suppress("unused")
        val objWithouId = object {
            val eid = Gid(0)
        }
        assertFailsWith<IllegalArgumentException> {
            findGidProp(objWithouId)
        }
    }

    @Test
    fun `Test destruction of entity with null scalar`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(NullableScalar::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))
        val facts = destruct(NullableScalar(null, null, 0), db::attr, gids)
        assertEquals(1, facts.size, "Only fact for placeholder should be generated")
    }

    @Test
    fun `Test bomb with nulls deconstruction`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(Country::class)
            entity(Bomb::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))
        val facts = destruct(createBombWithNulls(), db::attr, gids)
        assertEquals(41, facts.size)
    }

    @Test
    fun `Test bomb without nulls deconstruction`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(Country::class)
            entity(Bomb::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))
        val facts = destruct(createBombWithoutNulls(), db::attr, gids)
        assertEquals(100, facts.size)
    }

    // Support of self-refefencing entitys is under question now
    @Ignore
    @Test
    fun `Test destruction of self-referencing entity`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(Scientist::class)
            entity(Country::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))
        val s = Scientist(null, 1, "s", emptyList(), Country(null, "c", 0), null)
        s.reviewer = s

        val facts = destruct(s, db::attr, gids)
        assertEquals(6, facts.size)
        assertEquals(Gid(0, 7), facts.first { it.attr == Scientists.reviewer.name }.value)
    }

    @Test
    fun `Test bomb schema generation`() {
        val attrs = schema {
            entity(Bomb::class)
        }
        assertEquals(QBoolean.code, attrs[0].type)
        assertEquals(QBoolean.list().code, attrs[1].type)
        assertEquals(QBoolean.list().code, attrs[2].type)
        assertEquals(QByte.code, attrs[3].type)
        assertEquals(QByte.list().code, attrs[4].type)
        assertEquals(QByte.list().code, attrs[5].type)
        assertEquals(QBytes.code, attrs[6].type)
        assertEquals(QBytes.list().code, attrs[7].type)
        assertEquals(QBytes.list().code, attrs[8].type)
        assertEquals(QRef.list().code, attrs[9].type)
        assertEquals(QRef.list().code, attrs[10].type)
        assertEquals(QRef.code, attrs[11].type)
        assertEquals(QInt.code, attrs[12].type)
        assertEquals(QInt.list().code, attrs[13].type)
        assertEquals(QInt.list().code, attrs[14].type)
        assertEquals(QLong.code, attrs[15].type)
        assertEquals(QLong.list().code, attrs[16].type)
        assertEquals(QLong.list().code, attrs[17].type)
        assertEquals(QBoolean.code, attrs[18].type)
        assertEquals(QBoolean.list().code, attrs[19].type)
        assertEquals(QBoolean.list().code, attrs[20].type)
        assertEquals(QRef.list().code, attrs[21].type)
        assertEquals(QRef.list().code, attrs[22].type)
        assertEquals(QRef.code, attrs[23].type)
        assertEquals(QBoolean.code, attrs[24].type)
        assertEquals(QRef.code, attrs[25].type)
        assertEquals(QRef.code, attrs[26].type)
        assertEquals(QBoolean.code, attrs[27].type)
        assertEquals(QByte.code, attrs[28].type)
        assertEquals(QBytes.code, attrs[29].type)
        assertEquals(QRef.code, attrs[30].type)
        assertEquals(QInt.code, attrs[31].type)
        assertEquals(QLong.code, attrs[32].type)
        assertEquals(QString.code, attrs[33].type)
        assertEquals(QString.code, attrs[34].type)
        assertEquals(QString.list().code, attrs[35].type)
        assertEquals(QString.list().code, attrs[36].type)
    }
}

