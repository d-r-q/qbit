package qbit.typing

import qbit.*
import qbit.api.*
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.*
import qbit.factorization.*
import qbit.index.Index
import qbit.index.IndexDb
import qbit.query.EagerQuery
import qbit.query.GraphQuery
import qbit.schema.schema
import kotlin.test.*

data class Addr(val id: Long?, val addr: String)

data class MUser(
        val id: Long? = null,
        val login: String,
        val strs: List<String>,
        val addr: Addr,
        val optAddr: Addr?,
        val addrs: List<Addr>
)

data class ListOfNullablesHolder(val id: Long?, val nullables: ListOfNullables)

class MappingTest {

    @Test
    fun `Test simple entity mapping`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(MUser::class) {
                uniqueString(it::login)
            }
            entity(Addr::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))

        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                addr = Addr(null, "addr"),
                optAddr = Addr(null, "optAddr"),
                addrs = listOf(Addr(null, "lstAddr"))
        )

        val facts = destruct(user, db::attr, gids)
        val db2 = IndexDb(db.index.addFacts(facts))
        val se = db2.pull(facts.entityFacts[user]!!.first().gid)
        val lazyTyping = Typing(se!!, GraphQuery(MUser::class, emptyMap()), MUser::class)
        val u = lazyTyping.instantiate(se, MUser::class)
        assertEquals("login", u.login)
        assertEquals(listOf("str1", "str2"), u.strs)
        assertEquals("addr", u.addr.addr)
        assertNull(u.optAddr)
        assertEquals("lstAddr", u.addrs[0].addr)

        val eagerTyping = Typing(se, EagerQuery(), MUser::class)
        val fullUser = eagerTyping.instantiate(se, MUser::class)
        assertEquals("optAddr", fullUser.optAddr!!.addr)
    }

    @Test
    fun `Test entity graph with mulitple entity types mapping`() {

        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(MUser::class) {
                uniqueString(it::login)
            }
            entity(Addr::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))

        val addr = Addr(null, "addr")
        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                addr = addr,
                optAddr = addr,
                addrs = listOf(addr)
        )
        val facts = destruct(user, db::attr, gids)
        val db2 = IndexDb(db.index.addFacts(facts))

        val se = db2.pull(facts.entityFacts[user]!!.first().gid)!!
        val eagerTyping = Typing(se, EagerQuery(), MUser::class)
        val fullUser = eagerTyping.instantiate(se, MUser::class)

        assertTrue(fullUser.addr == fullUser.optAddr && fullUser.optAddr == fullUser.addrs[0])
        assertTrue(fullUser.addr === fullUser.optAddr && fullUser.optAddr === fullUser.addrs[0])
    }

    @Ignore
    @Test
    fun `test multiple states of entity in entity graph is prohibited`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(MUser::class)
            entity(Addr::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))

        val addr = Addr(1, "addr")
        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                addr = addr,
                optAddr = addr,
                addrs = listOf(addr)
        )
        val userWithAddr = user.copy(addr = user.addr.copy(addr = "newAddr"))

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
    fun `destruction of entity with list of nullable elements in props should fail`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(ListOfNullables::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))
        val ex = assertFailsWith<QBitException> {
            destruct(ListOfNullables(null, listOf(null), listOf(null)), db::attr, gids)
        }
        assertEquals("List of nullable elements is not supported. Properties: ListOfNullables.(lst,refLst)", ex.message)
    }

    @Test
    fun `destruction of graph with entity with list of nullable elements in props should fail`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(ListOfNullablesHolder::class)
            entity(ListOfNullables::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))
        val ex = assertFailsWith<QBitException> {
            destruct(ListOfNullablesHolder(null, ListOfNullables(null, listOf(null), listOf(null))), db::attr, gids)
        }
        assertEquals("List of nullable elements is not supported. Properties: ListOfNullables.(lst,refLst)", ex.message)
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
    fun `Test destruction of entity with null list`() {
        val gids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(NullableList::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))
        val facts = destruct(NullableList(null, null, 0), db::attr, gids)
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
        assertEquals(56, facts.size)
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
        assertEquals(134, facts.size)
    }

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
        assertEquals(QZonedDateTime.code, attrs[12].type)
        assertEquals(QZonedDateTime.list().code, attrs[13].type)
        assertEquals(QZonedDateTime.list().code, attrs[14].type)
        assertEquals(QDecimal.code, attrs[15].type)
        assertEquals(QDecimal.list().code, attrs[16].type)
        assertEquals(QDecimal.list().code, attrs[17].type)
        assertEquals(QInstant.code, attrs[18].type)
        assertEquals(QInstant.list().code, attrs[19].type)
        assertEquals(QInstant.list().code, attrs[20].type)
        assertEquals(QInt.code, attrs[21].type)
        assertEquals(QInt.list().code, attrs[22].type)
        assertEquals(QInt.list().code, attrs[23].type)
        assertEquals(QLong.code, attrs[24].type)
        assertEquals(QLong.list().code, attrs[25].type)
        assertEquals(QLong.list().code, attrs[26].type)
        assertEquals(QBoolean.code, attrs[27].type)
        assertEquals(QBoolean.list().code, attrs[28].type)
        assertEquals(QBoolean.list().code, attrs[29].type)
        assertEquals(QRef.list().code, attrs[30].type)
        assertEquals(QRef.list().code, attrs[31].type)
        assertEquals(QRef.code, attrs[32].type)
        assertEquals(QBoolean.code, attrs[33].type)
        assertEquals(QRef.code, attrs[34].type)
        assertEquals(QRef.code, attrs[35].type)
        assertEquals(QBoolean.code, attrs[36].type)
        assertEquals(QByte.code, attrs[37].type)
        assertEquals(QBytes.code, attrs[38].type)
        assertEquals(QRef.code, attrs[39].type)
        assertEquals(QZonedDateTime.code, attrs[40].type)
        assertEquals(QDecimal.code, attrs[41].type)
        assertEquals(QInstant.code, attrs[42].type)
        assertEquals(QInt.code, attrs[43].type)
        assertEquals(QLong.code, attrs[44].type)
        assertEquals(QString.code, attrs[45].type)
        assertEquals(QString.code, attrs[46].type)
        assertEquals(QString.list().code, attrs[47].type)
        assertEquals(QString.list().code, attrs[48].type)
    }

    @Test
    fun `Test destruction of graph with different objects for the same entity state`() {
        val gids = Gid(0, 2).nextGids()
        val testSchema = schema {
            entity(Country::class)
            entity(Scientist::class)
            entity(ResearchGroup::class)
        }
        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, EmptyDb::attr, gids) }))
        val s1 = Scientist(0, 1, "Name", emptyList(), Country(1, "Country", null), null)
        val s2 = Scientist(0, 1, "Name", emptyList(), Country(1, "Country", null), null)
        val rg = ResearchGroup(null, listOf(s1, s2))
        assertEquals(1, destruct(rg, db::attr, gids).filter { it.gid == Gid(0) && it.attr == Scientists.extId.name }.size)
    }
}

