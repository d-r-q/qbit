package qbit.mapping

import qbit.IndexDb
import qbit.dbOf
import qbit.model.EID
import qbit.model.ListAttr
import qbit.model.QString
import qbit.model.ScalarAttr
import qbit.ns.ns
import qbit.nullHash
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

data class Addr(val id: Long?, val addr: String)

data class User(
        val id: Long?,
        val login: String,
        val strs: List<String>,
        val addr: Addr,
        val optAddr: Addr?,
        val addrs: List<Addr>
)

class MappingTest {

    @Test
    fun test() {
        val eids = EID(0, 0).nextEids()
        val schema = schemaFor(User::class) + schemaFor(Addr::class)
        val db = dbOf(eids, *schema.toTypedArray())
        val facts = destruct(User(null, "login", listOf("str1", "str2"), Addr(null, "addr"), Addr(null, "optAddr"), listOf(Addr(null, "lstAddr"))), db, eids)
        val db2 = IndexDb((db as IndexDb).index.addFacts(facts), nullHash)
        val u = reconstruct(User::class, facts.filter { it.eid.eid == 6}, db2)
        assertEquals("login", u.login)
        assertEquals(listOf("str1", "str2"), u.strs)
        assertEquals("addr", u.addr.addr)
        assertNull(u.optAddr)
        assertEquals("lstAddr", u.addrs[0].addr)

        val fullUser = reconstruct(Query(User::class, mapOf(User::optAddr.name to null)), facts.filter { it.eid.eid == 6 }, db2)
        assertEquals("optAddr", fullUser.optAddr!!.addr)
    }

}