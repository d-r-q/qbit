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
            assertEquals("Im updated", it.pullT<Scientist>(eCodd.id!!)!!.name)
        }
    }

    @Test
    fun testUniqueConstraintCheck() {
        val conn = setupTestData()
        try {
            conn.persist(Scientist(null, eCodd.externalId, "", emptyList(), uk))
            fail("QBitException expected")
        } catch (e: QBitException) {
            assertTrue(e.message?.contains("Duplicate") ?: false)
        }
    }

    @Test
    fun testDelete() {
        val conn = setupTestData()
        var eCodd = conn.db().pullT<Scientist>(eCodd.gid)!!
        val newExtId = eCodd.externalId shl 10
        eCodd = eCodd.copy(externalId = newExtId)
        conn.persist(eCodd)

        val pulledECodd = conn.db().pullT<Scientist>(eCodd.gid)!!
        assertEquals(newExtId, pulledECodd.externalId)

        conn.persist(pulledECodd.tombstone)

        val deletedPulledE2 = conn.db().pull(eCodd.gid)
        assertNull(deletedPulledE2)
        assertEquals(0, conn.db().query(attrIs(Scientists.extId, eCodd.externalId)).count())
        assertEquals(0, conn.db().query(attrIs(Scientists.extId, newExtId)).count())
    }

    @Test
    fun testPersistEntityViaRef() {
        val conn = setupTestData()

        var su = Country(null, "USSR", 146_000_000)
        var aErshov = Scientist(null, 0, "Andrey Ershov", listOf("Kompilatorshik"), su)

        conn.persist(aErshov)
        aErshov = conn.db().queryT<Scientist>(attrIs(Scientists.extId, 0)).first()
        su = aErshov.country
        assertNotNull(su.id)
        assertEquals("USSR", su.name)
    }

    @Test
    fun `Test pulling of referenced entity()`() {
        val conn = setupTestData()
        assertEquals("Russia", conn.db().pullT<Region>(nsk.gid)!!.country.name)
    }

    @Test
    fun `Test persistence and pulling of entities cycle`() {
        val conn = setupTestData()
        val pChenReviewed = pChen.copy()
        val eCoddReviewed = eCodd.copy(reviewer = pChenReviewed)
        val mStonebreakerReviewed = mStonebreaker.copy(reviewer = eCoddReviewed)
        pChenReviewed.reviewer = mStonebreakerReviewed

        conn.persist(pChenReviewed)

        val pc = conn.db().queryT<Scientist>(attrIs(Scientists.extId, pChen.externalId), fetch = Eager).first()

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

    @Test
    fun `Test updating entity with unique attribute (it shouldn't treated as unique constraint violation)`() {
        val conn = setupTestData()
        conn.persist(eCodd.copy(name = "Not A Codd"))
        val updatedCodd = conn.db().pullT<Scientist>(eCodd.gid)!!
        assertEquals(1, updatedCodd.externalId)
        assertEquals("Not A Codd", updatedCodd.name)
    }

    @Test
    fun `Test persistence entity with scalar list attribute`() {
        val conn = setupTestData()
        conn.persist(Scientist(null, 5, "Name", listOf("nick1", "nick2"), ru))
        val user = conn.db().queryT<Scientist>(attrIs(Scientists.extId, 5)).first()
        assertEquals(listOf("nick1", "nick2"), user.nicks)
    }

    @Test
    fun `Test persistence entity with ref list attribute`() {
        val conn = setupTestData()
        conn.persist(ResearchGroup(null, listOf(eCodd, pChen)))
        val rg = conn.db().queryT<ResearchGroup>(hasAttr(ResearchGroups.members)).first()
        assertEquals(listOf(eCodd, pChen), rg.members)
    }

    @Test
    fun `Test deletion of scalar list element`() {
        val conn = setupTestData()
        conn.persist(eCodd.copy(nicks = eCodd.nicks.drop(1)))
        val updatedCodd = conn.db().pullT<Scientist>(eCodd.gid)!!
        assertEquals(listOf("tabulator"), updatedCodd.nicks)
    }

    @Test
    fun `Test reoreder of ref list elements`() {
        val conn = setupTestData()
        conn.persist(ResearchGroup(null, listOf(eCodd, pChen)))
        val researchGroup = conn.db().queryT<ResearchGroup>(hasAttr(ResearchGroups.members)).first()
        conn.persist(researchGroup.copy(members = researchGroup.members.reversed()))
        val updatedRG = conn.db().queryT<ResearchGroup>(hasAttr(ResearchGroups.members)).first()
        assertEquals(listOf(pChen, eCodd), updatedRG.members)
    }

    @Ignore
    @Test
    fun `Test scalar list clearing`() {
        val conn = setupTestData()
        conn.persist(eCodd.copy(nicks = emptyList()))
        val updatedCodd = conn.db().pullT<Scientist>(eCodd.gid)!!
        assertEquals(emptyList(), updatedCodd.nicks)
    }

}