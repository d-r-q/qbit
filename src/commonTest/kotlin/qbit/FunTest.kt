package qbit

import qbit.mapping.gid
import qbit.model.tombstone
import kotlin.test.*


class FunTest {

    @Test
    fun testUpdate() {
        val conn = setupTestData()
        conn.persist(eCodd.copy(name = "Im updated"))
        conn.db {
            assertEquals("Im updated", it.pullT<User>(eCodd.id!!)!!.name)
        }
    }

    @Test
    fun testUniqueConstraintCheck() {
        val conn = setupTestData()
        try {
            conn.persist(User(null, eCodd.externalId, "", emptyList(), uk))
            fail("QBitException expected")
        } catch (e: QBitException) {
            assertTrue(e.message?.contains("Duplicate") ?: false)
        }
    }

    @Test
    fun testDelete() {
        val conn = setupTestData()
        var eCodd = conn.db().pullT<User>(eCodd.gid)!!
        val newExtId = eCodd.externalId shl 10
        eCodd = eCodd.copy(externalId = newExtId)
        conn.persist(eCodd)

        val pulledECodd = conn.db().pullT<User>(eCodd.gid)!!
        assertEquals(newExtId, pulledECodd.externalId)

        conn.persist(pulledECodd.tombstone)

        val deletedPulledE2 = conn.db().pull(eCodd.gid)
        assertNull(deletedPulledE2)
        assertEquals(0, conn.db().query(attrIs(Users.extId, eCodd.externalId)).count())
        assertEquals(0, conn.db().query(attrIs(Users.extId, newExtId)).count())
    }

}