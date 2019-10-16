package qbit

import qbit.mapping.gid
import qbit.model.tombstone
import qbit.storage.MemStorage
import qbit.storage.NodesStorage
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

    @Test
    fun testPersistEntityViaRef() {
        val conn = setupTestData()

        var su = Country(null, "USSR", 146_000_000)
        var aErshov = User(null, 0, "Andrey Ershov", listOf("Kompilatorshik"), su)

        conn.persist(aErshov)
        aErshov = conn.db().queryT<User>(attrIs(Users.extId, 0)).first()
        su = aErshov.country
        assertNotNull(su.id)
        assertEquals("USSR", su.name)
    }


    @Test
    fun `Test persistence and pulling of entities cycle`() {
        val conn = setupTestData()
        val pChenReviewed = pChen.copy()
        val eCoddReviewed = eCodd.copy(reviewer = pChenReviewed)
        val mStonebreakerReviewed = mStonebreaker.copy(reviewer = eCoddReviewed)
        pChenReviewed.reviewer = mStonebreakerReviewed

        conn.persist(pChenReviewed)

        val pc = conn.db().queryT<User>(attrIs(Users.extId, pChen.externalId), fetch = Eager).first()

        assertEquals("Peter Chen", pc.name)
        assertEquals("Michael Stonebreaker", pc.reviewer?.name)
        assertEquals("Edgar Codd", pc.reviewer?.reviewer?.name)
    }


    @Test
    fun `When not changed entity is stored, qbit should not write new transaction`() {
        val conn = setupTestData()
        val head = conn.head
        conn.persist(eCodd)

        assertEquals(head, conn.head)
    }

    @Test
    fun `When new entity referencing not changed entity is stored, qbit should write only new entity`() {
        val storage = MemStorage()
        val conn = setupTestData(storage)
        conn.persist(Region(null, "Kemerovskaya obl.", ru))

        assertEquals(5, NodesStorage(storage).load(NodeRef(conn.head))!!.data.trx.size, "5 facts (2 for region and 3 for instance) expected")
    }

}