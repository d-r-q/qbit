package qbit

import qbit.db.Instance
import qbit.typing.gid
import qbit.model.Gid
import qbit.model.tombstone
import qbit.storage.MemStorage
import qbit.storage.NodesStorage
import qbit.db.qbit
import qbit.index.*
import qbit.serialization.NodeRef
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
        var eCodd = conn.db().pullT<Scientist>(eCodd.gid!!)!!
        val newExtId = eCodd.externalId shl 10
        eCodd = eCodd.copy(externalId = newExtId)
        conn.persist(eCodd)

        val pulledECodd = conn.db().pullT<Scientist>(eCodd.gid!!)!!
        assertEquals(newExtId, pulledECodd.externalId)

        val ts = pulledECodd.tombstone
        val (stored) = conn.persist(ts)
        assertSame(ts, stored)

        val deletedPulledE2 = conn.db().pull(eCodd.gid!!)
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
        assertEquals("Russia", conn.db().pullT<Region>(nsk.gid!!)!!.country.name)
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
        val updatedCodd = conn.db().pullT<Scientist>(eCodd.gid!!)!!
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
        val updatedCodd = conn.db().pullT<Scientist>(eCodd.gid!!)!!
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
        val updatedCodd = conn.db().pullT<Scientist>(eCodd.gid!!)!!
        assertEquals(emptyList(), updatedCodd.nicks)
    }

    @Test
    fun `Storage of different states of same entity should fail`() {
        val conn = setupTestData()
        val pChen2 = pChen.copy(name = "Not A Chen")
        val rg = ResearchGroup(null, listOf(pChen, pChen2))
        val ex = assertFailsWith<QBitException> {
            conn.persist(rg)
        }
        assertTrue(ex.message?.contains("Entity ${pChen.gid} has several different states to store") == true)
    }

    @Test
    fun `Storage of two entities with same value of unique attribute should fail with uniqueness violation exception`() {
        val conn = setupTestData()
        val pChen2 = pChen.copy(externalId = 100)
        val eCodd2 = eCodd.copy(externalId = 100)
        val rg = ResearchGroup(null, listOf(eCodd2, pChen2))
        val ex = assertFailsWith<QBitException> {
            conn.persist(rg)
        }
        assertTrue(ex.message!!.contains("Uniqueness violation for attr (.qbit.Scientist/externalId, 100), entities"))
    }

    @Ignore
    @Test
    fun `Storage of entity with empty list should preserve list`() {
        val conn = setupTestData()
        conn.persist(NullableList(null, emptyList(), 0))
        val stored = conn.db().queryT<NullableList>(attrIs(NullableLists.placeholder, 0L)).first()
        assertEquals(emptyList(), stored.lst)
    }

    @Ignore
    @Test
    fun `Qbit should forbid externally generated gids`() {
        val conn = setupTestData()
        val ex = assertFailsWith<QBitException> {
            conn.persist(IntEntity(Gid(1, 81).value(), 0))
        }
        assertEquals("", ex.message)
    }

    @Ignore
    @Test
    fun `Peristance of entity with all attr = null should actually persist the entity`() {
        val conn = setupTestSchema()
        val (stored) = conn.persist(NullableScalarWithoutPlaceholder(null, null))
        assertNotNull(stored)
        assertNotNull(conn.db().pull(stored.gid!!))
        assertNull(conn.db().pullT<NullableScalarWithoutPlaceholder>(stored.gid!!)!!.scalar)
    }

    @Ignore
    @Test
    fun `Test persistence of entity without attributes`() {
        // Consider storing entities without attributes, to enable use case, when untyped attributes add later to the entity
        val conn = setupTestSchema()
        val (stored) = conn.persist(EntityWithoutAttrs(null))
        assertNotNull(stored)
        assertNotNull(conn.db().pull(stored.gid!!))
    }

    @Test
    fun `Reopening existing storage should preserve state`() {
        val storage = MemStorage()
        setupTestSchema(storage)

        val conn1 = qbit(storage)
        assertNotNull(conn1.db().attr(Scientists.name.name))
        conn1.persist(IntEntity(null, 2))

        val conn2 = qbit(storage)
        assertNotNull(conn2.db().query(attrIs(IntEntities.int, 2)).firstOrNull())
    }

    @Test
    fun `Test bomb with nulls handling`() {
        val conn = setupTestData()
        val bomb = createBombWithNulls()
        conn.persist(bomb)
        val storedBomb = conn.db().queryT<Bomb>(hasAttr(Bombs.bool), fetch = Eager).firstOrNull()
        assertNotNull(storedBomb)
        assertNull(storedBomb.optBool)
        assertNull(storedBomb.boolListOpt)
        assertNull(storedBomb.mutOptBool)
        assertNull(storedBomb.mutBoolListOpt)

        assertNull(storedBomb.optByte)
        assertNull(storedBomb.byteListOpt)
        assertNull(storedBomb.optInt)
        assertNull(storedBomb.intListOpt)
        assertNull(storedBomb.optLong)
        assertNull(storedBomb.longListOpt)
        assertNull(storedBomb.optInst)
        assertNull(storedBomb.instListOpt)
        assertNull(storedBomb.optDec)
        assertNull(storedBomb.decListOpt)
        assertNull(storedBomb.optDateTime)
        assertNull(storedBomb.dateTimeListOpt)
        assertNull(storedBomb.optStr)
        assertNull(storedBomb.strListOpt)
        assertNull(storedBomb.optBytes)
        assertNull(storedBomb.bytesListOpt)
        assertNull(storedBomb.optCountry)
        assertNull(storedBomb.countriesListOpt)
        assertNull(storedBomb.mutOptCountry)
        assertNull(storedBomb.mutCountriesListOpt)
    }

    @Test
    fun `Test bomb without nulls handling`() {
        val conn = setupTestData()
        val inst = conn.db().pullT<Instance>(theInstanceGid)!!
        val bomb = createBombWithoutNulls()
        conn.persist(bomb)
        val storedBomb = conn.db().queryT<Bomb>(hasAttr(Bombs.bool), fetch = Eager).first()

        assertEquals(bomb.bool, storedBomb.bool)
        assertEquals(bomb.optBool, storedBomb.optBool)
        assertEquals(bomb.boolList, storedBomb.boolList)
        // todo: assertEquals(bomb.boolListOpt, storedBomb.boolListOpt)

        assertEquals(bomb.mutBool, storedBomb.mutBool)
        assertEquals(bomb.mutOptBool, storedBomb.mutOptBool)
        assertEquals(bomb.mutBoolList, storedBomb.mutBoolList)
        assertEquals(bomb.mutBoolListOpt, storedBomb.mutBoolListOpt)

        assertEquals(bomb.byte, storedBomb.byte)
        assertEquals(bomb.optByte, storedBomb.optByte)
        assertEquals(bomb.byteList, storedBomb.byteList)
        // todo: assertEquals(bomb.byteListOpt, storedBomb.byteListOpt)

        assertEquals(bomb.int, storedBomb.int)
        assertEquals(bomb.optInt, storedBomb.optInt)
        assertEquals(bomb.intList, storedBomb.intList)
        assertEquals(bomb.intListOpt, storedBomb.intListOpt)

        assertEquals(bomb.long, storedBomb.long)
        assertEquals(bomb.optLong, storedBomb.optLong)
        assertEquals(bomb.longList, storedBomb.longList)
        assertEquals(bomb.longListOpt, storedBomb.longListOpt)

        // there is some strange bug with instant (de)serealization on openjdk 11, see
        // 4e072eb8
        // 39366672
        // https://travis-ci.com/d-r-q/qbit/builds/132566841
        // https://travis-ci.com/d-r-q/qbit/builds/95747234
        // https://travis-ci.com/d-r-q/qbit/builds/95749078
        assertEquals(bomb.inst.getNano() / 1000000, storedBomb.inst.getNano() / 1000000)
        assertEquals(bomb.optInst!!.getNano() / 1000000, storedBomb.optInst!!.getNano() / 1000000)
        assertEquals(bomb.instList, storedBomb.instList)
        // todo: assertEquals(bomb.instListOpt, storedBomb.instListOpt)

        assertEquals(bomb.dec, storedBomb.dec)
        assertEquals(bomb.optDec, storedBomb.optDec)
        assertEquals(bomb.decList, storedBomb.decList)
        assertEquals(bomb.decListOpt, storedBomb.decListOpt)

        assertEquals(bomb.dateTime, storedBomb.dateTime)
        assertEquals(bomb.optDateTime, storedBomb.optDateTime)
        assertEquals(bomb.dateTimeList, storedBomb.dateTimeList)
        // todo: assertEquals(bomb.dateTimeListOpt, storedBomb.dateTimeListOpt)

        assertEquals(bomb.str, storedBomb.str)
        assertEquals(bomb.optStr, storedBomb.optStr)
        assertEquals(bomb.strList, storedBomb.strList)
        assertEquals(bomb.strListOpt, storedBomb.strListOpt)

        assertArrayEquals(bomb.bytes, storedBomb.bytes)
        assertArrayEquals(bomb.optBytes, storedBomb.optBytes)
        assertTrue(bomb.bytesList.zip(storedBomb.bytesList).all { (a, b) -> a.contentEquals(b) })
        assertTrue(bomb.bytesListOpt!!.zip(storedBomb.bytesListOpt!!).all { (a, b) -> a.contentEquals(b) })

        assertEquals(bomb.country, storedBomb.country)
        assertEquals(bomb.optCountry, storedBomb.optCountry)
        assertEquals(listOf(Country(0, "Country", 0), Country(Gid(1, inst.nextEid + 1).value(), "Country3", 2)), storedBomb.countiesList)
        // todo: assertEquals(bomb.countriesListOpt, storedBomb.countriesListOpt)

        assertEquals(bomb.mutCountry, storedBomb.mutCountry)
        assertEquals(bomb.mutOptCountry, storedBomb.mutOptCountry)
        assertEquals(bomb.mutCountriesList, storedBomb.mutCountriesList)
        assertEquals(bomb.mutCountriesListOpt, storedBomb.mutCountriesListOpt)

        assertEquals(Gid(2, 102).value(), storedBomb.optBomb!!.id)
        assertEquals(storedBomb.id, storedBomb.optBomb?.optBomb?.id)
    }

}