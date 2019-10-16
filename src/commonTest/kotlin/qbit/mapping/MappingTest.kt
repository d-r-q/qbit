package qbit.mapping

import qbit.*
import qbit.model.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
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

class MappingTest {

    @Test
    fun test() {
        val eids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(MUser::class) {
                unique(it::login)
            }
            entity(Addr::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, emptyDb::attr, eids) }))

        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                addr = Addr(null, "addr"),
                optAddr = Addr(null, "optAddr"),
                addrs = listOf(Addr(null, "lstAddr"))
        )

        val facts = destruct(user, db::attr, eids)
        val db2 = IndexDb(db.index.addFacts(facts))
        val u = reconstruct(MUser::class, facts.filter { it.eid.eid == 6 }, db2)
        assertEquals("login", u.login)
        assertEquals(listOf("str1", "str2"), u.strs)
        assertEquals("addr", u.addr.addr)
        assertNull(u.optAddr)
        assertEquals("lstAddr", u.addrs[0].addr)

        val fullUser = reconstruct(GraphQuery(MUser::class, mapOf(MUser::optAddr.name to null)), facts.filter { it.eid.eid == 6 }, db2)
        assertEquals("optAddr", fullUser.optAddr!!.addr)

        schema {
            entity(MUser::class) {
                unique(it::login)
            }
        }
    }

    @Test
    fun test2() {

        val eids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(MUser::class) {
                unique(it::login)
            }
            entity(Addr::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, emptyDb::attr, eids) }))

        val addr = Addr(null, "addr")
        val user = MUser(
                login = "login",
                strs = listOf("str1", "str2"),
                addr = addr,
                optAddr = addr,
                addrs = listOf(addr)
        )
        val facts = destruct(user, db::attr, eids)
        val db2 = IndexDb(db.index.addFacts(facts))
        val fullUser = reconstruct(GraphQuery(MUser::class, mapOf(MUser::optAddr.name to null)), facts.filter { it.eid.eid == 6 }, db2)
        assertTrue(fullUser.addr == fullUser.optAddr && fullUser.optAddr == fullUser.addrs[0])
        assertTrue(fullUser.addr === fullUser.optAddr && fullUser.optAddr === fullUser.addrs[0])
    }

    @Ignore
    @Test
    fun `test multiple states of entity in entity graph is prohibited`() {
        val eids = Gid(0, 0).nextGids()

        val testSchema = schema {
            entity(MUser::class)
            entity(Addr::class)
        }

        val db = IndexDb(Index().addFacts(testSchema.flatMap { destruct(it, emptyDb::attr, eids) }))

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
            destruct(userWithAddr, db::attr, eids)
        }
    }

    @Test
    fun `Type of Any(class) is QRef`() {
        assertEquals(QRef, types[Any::class])
    }

    @Ignore
    @Test
    fun `Pulling entity of wrong type should fail with explanation`() {
        val eids = Gid(0, 0).nextGids()
        val addrFacts = destruct(Attr<String>("addr"), bootstrapSchema::get, eids)
        assertFailsWith<QBitException> {
            reconstruct(MUser::class, addrFacts, emptyDb)
        }
    }

    @Test
    fun `findGidProp should return only properties with name 'id'`() {
        val objWithouId = object {
            val eid = Gid(0)
        }
        assertFailsWith<IllegalArgumentException> {
            findGidProp(objWithouId)
        }
    }

}

fun schema(body: SchemaBuilder.() -> Unit): List<Attr<Any>> {
    val scb = SchemaBuilder()
    scb.body()
    return scb.attrs
}

class SchemaBuilder {

    internal val attrs: MutableList<Attr<Any>> = ArrayList()

    fun <T : Any> entity(type: KClass<T>, body: EntityBuilder.(T) -> Unit = {}) {
        val eb = EntityBuilder(type)
        eb.body(default(type))
        attrs.addAll(schemaFor(type, eb.uniqueProps))
    }

}

class EntityBuilder(internal val type: KClass<*>) {

    internal val uniqueProps = HashSet<String>()

    fun unique(prop: KProperty0<*>) {
        uniqueProps.add(type.attrName(prop))
    }

}

