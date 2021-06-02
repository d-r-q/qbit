package qbit

import kotlinx.coroutines.delay
import qbit.api.QBitException
import qbit.api.db.*
import qbit.api.gid.Gid
import qbit.index.InternalDb
import qbit.platform.runBlocking
import qbit.serialization.CommonNodesStorage
import qbit.serialization.NodeRef
import qbit.storage.MemStorage
import qbit.test.model.*
import kotlin.js.JsName
import kotlin.test.*


class FunTest {

    @Test
    fun testUpdate() {
        runBlocking {
            val conn = setupTestData()
            conn.persist(eCodd.copy(name = "Im updated"))
            conn.db {
                assertEquals("Im updated", it.pull<Scientist>(eCodd.id!!)!!.name)
            }
        }
    }

    @Test
    fun testUniqueConstraintCheck() {
        runBlocking {
            val conn = setupTestData()
            try {
                conn.persist(Scientist(null, eCodd.externalId, "", emptyList(), uk))
                fail("QBitException expected")
            } catch (e: QBitException) {
                assertTrue(e.message?.contains("Duplicate") ?: false)
            }
        }
    }

    @Test
    fun testDelete() {
        runBlocking {
            val conn = setupTestData()
            var eCodd = conn.db().pull<Scientist>(eCodd.id!!)!!
            val newExtId = eCodd.externalId shl 10
            eCodd = eCodd.copy(externalId = newExtId)
            conn.persist(eCodd)

            val pulledECodd = conn.db().pull<Scientist>(eCodd.id!!)!!
            assertEquals(newExtId, pulledECodd.externalId)

            val ts = Tombstone(pulledECodd.id!!)
            val (stored) = conn.persist(ts)
            assertSame(ts, stored)

            val deletedPulledE2 = conn.db().pull<Scientist>(eCodd.id!!)
            assertNull(deletedPulledE2)
            assertEquals(0, conn.db().query<Scientist>(attrIs(Scientists.extId, eCodd.externalId)).count())
            assertEquals(0, conn.db().query<Scientist>(attrIs(Scientists.extId, newExtId)).count())
        }
    }

    @Test
    fun testPersistEntityViaRef() {
        runBlocking {
            val conn = setupTestData()

            var su = Country(null, "USSR", 146_000_000)
            var aErshov = Scientist(null, 0, "Andrey Ershov", listOf("Kompilatorshik"), su)

            conn.persist(aErshov)
            aErshov = conn.db().query<Scientist>(attrIs(Scientists.extId, 0)).first()
            su = aErshov.country
            assertNotNull(su.id)
            assertEquals("USSR", su.name)
        }
    }

    @JsName("Test_pulling_of_referenced_entity")
    @Test
    fun `Test pulling of referenced entity()`() {
        runBlocking {
            val conn = setupTestData()
            assertEquals("Russia", conn.db().pull<Region>(nsk.id!!)!!.country.name)
        }
    }

    @Ignore
    @JsName("Test_persistence_and_pulling_of_entities_cycle")
    @Test
    fun `Test persistence and pulling of entities cycle`() {
        runBlocking {
            val conn = setupTestData()
            val pChenReviewed = pChen.copy()
            val eCoddReviewed = eCodd.copy(reviewer = pChenReviewed)
            val mStonebreakerReviewed = mStonebreaker.copy(reviewer = eCoddReviewed)
            pChenReviewed.reviewer = mStonebreakerReviewed

            conn.persist(pChenReviewed)

            val pc = conn.db().query<Scientist>(attrIs(Scientists.extId, pChen.externalId), fetch = Eager).first()

            assertEquals("Peter Chen", pc.name)
            assertEquals("Michael Stonebreaker", pc.reviewer?.name)
            assertEquals("Edgar Codd", pc.reviewer?.reviewer?.name)
        }
    }

    @JsName("When_not_changed_entity_is_stored_qbit_should_not_write_new_transaction")
    @Test
    fun `When not changed entity is stored qbit should not write new transaction`() {
        runBlocking {
            val conn = setupTestData()
            val head = conn.head
            conn.persist(eCodd)

            assertEquals(head, conn.head)
        }
    }

    @JsName("When_new_entity_referencing_not_changed_entity_is_stored_qbit_should_write_only_new_entity")
    @Test
    fun `When new entity referencing not changed entity is stored, qbit should write only new entity`() {
        runBlocking {
            val storage = MemStorage()
            val conn = setupTestData(storage)
            conn.persist(Region(null, "Kemerovskaya obl.", ru))

            assertEquals(
                5,
                CommonNodesStorage(storage).load(NodeRef(conn.head))!!.data.trxes.size,
                "5 facts (2 for region and 3 for instance) expected"
            )
        }
    }

    @JsName("Test_updating_entity_with_unique_attribute_it_shouldnt_treated_as_unique_constraint_violation")
    @Test
    fun `Test updating entity with unique attribute (it shouldn't treated as unique constraint violation)`() {
        runBlocking {
            val conn = setupTestData()
            conn.persist(eCodd.copy(name = "Not A Codd"))
            val updatedCodd = conn.db().pull<Scientist>(eCodd.id!!)!!
            assertEquals(1, updatedCodd.externalId)
            assertEquals("Not A Codd", updatedCodd.name)
        }
    }

    @JsName("Test_persistence_entity_with_scalar_list_attribute")
    @Test
    fun `Test persistence entity with scalar list attribute`() {
        runBlocking {
            val conn = setupTestData()
            conn.persist(Scientist(null, 5, "Name", listOf("nick1", "nick2"), ru))
            val user = conn.db().query<Scientist>(attrIs(Scientists.extId, 5)).first()
            assertEquals(listOf("nick1", "nick2"), user.nicks)
        }
    }

    @JsName("Test_persistence_entity_with_ref_list_attribute")
    @Test
    fun `Test persistence entity with ref list attribute`() {
        runBlocking {
            val conn = setupTestData()
            conn.persist(ResearchGroup(null, listOf(eCodd, pChen)))
            val rg = conn.db().query<ResearchGroup>(hasAttr(ResearchGroups.members)).first()
            assertEquals(listOf(eCodd, pChen), rg.members)
        }
    }

    @JsName("Test_deletion_of_scalar_list_element")
    @Test
    fun `Test deletion of scalar list element`() {
        runBlocking {
            val conn = setupTestData()
            conn.persist(eCodd.copy(nicks = eCodd.nicks.drop(1)))
            val updatedCodd = conn.db().pull<Scientist>(eCodd.id!!)!!
            assertEquals(listOf("tabulator"), updatedCodd.nicks)
        }
    }

    @JsName("Test_reoreder_of_ref_list_elements")
    @Test
    fun `Test reoreder of ref list elements`() {
        runBlocking {
            val conn = setupTestData()
            conn.persist(ResearchGroup(null, listOf(eCodd, pChen)))
            val researchGroup = conn.db().query<ResearchGroup>(hasAttr(ResearchGroups.members)).first()
            conn.persist(researchGroup.copy(members = researchGroup.members.reversed()))
            val updatedRG = conn.db().query<ResearchGroup>(hasAttr(ResearchGroups.members)).first()
            assertEquals(listOf(pChen, eCodd), updatedRG.members)
        }
    }

    @Ignore
    @JsName("Test_scalar_list_clearing")
    @Test
    fun `Test scalar list clearing`() {
        runBlocking {
            val conn = setupTestData()
            conn.persist(eCodd.copy(nicks = emptyList()))
            val updatedCodd = conn.db().pull<Scientist>(eCodd.id!!)!!
            assertEquals(emptyList(), updatedCodd.nicks)
        }
    }

    @JsName("Storage_of_different_states_of_same_entity_should_fail")
    @Test
    fun `Storage of different states of same entity should fail`() {
        runBlocking {
            val conn = setupTestData()
            val pChen2 = pChen.copy(name = "Not A Chen")
            val rg = ResearchGroup(null, listOf(pChen, pChen2))
            val ex = assertFailsWith<QBitException> {
                conn.persist(rg)
            }
            assertTrue(ex.message?.contains("Entity ${Gid(pChen.id!!)} has several different states to store") == true)
        }
    }

    @Ignore
    @JsName("Storage_of_the_same_state_of_same_entity_should_store_single_entity")
    @Test
    fun `Storage of the same state of same entity should store single entity`() {
        runBlocking {
            val conn = setupTestData()
            val pChen2 = pChen.copy()
            val rg = ResearchGroup(null, listOf(pChen, pChen2))
            val (persitstedRg) = conn.persist(rg)
            val repulledRg = conn.db().pull<ResearchGroup>(persitstedRg!!.id!!)
            assertNotNull(repulledRg)
            assertEquals(2, repulledRg.members.size)
            assertEquals(repulledRg.members[0], repulledRg.members[1])
        }
    }

    @JsName("Storage_of_two_entities_with_same_value_of_unique_attribute_should_fail_with_uniqueness_violation_exception")
    @Test
    fun `Storage of two entities with same value of unique attribute should fail with uniqueness violation exception`() {
        runBlocking {
            val conn = setupTestData()
            val pChen2 = pChen.copy(externalId = 100)
            val eCodd2 = eCodd.copy(externalId = 100)
            val rg = ResearchGroup(null, listOf(eCodd2, pChen2))
            val ex = assertFailsWith<QBitException> {
                conn.persist(rg)
            }
            assertTrue(
                ex.message!!.contains("Uniqueness violation for attr (Scientist/externalId, 100), entities"),
                "Message ${ex.message} doesn't contains expected explanation"
            )
        }
    }

    @Ignore
    @JsName("Storage_of_entity_with_empty_list_should_preserve_list")
    @Test
    fun `Storage of entity with empty list should preserve list`() {
        runBlocking {
            val conn = setupTestData()
            conn.persist(NullableList(null, emptyList(), 0))
            val stored = conn.db().query<NullableList>(attrIs(NullableLists.placeholder, 0L)).first()
            assertEquals(emptyList(), stored.lst)
        }
    }

    @Ignore
    @JsName("Qbit_should_forbid_externally_generated_gids")
    @Test
    fun `Qbit should forbid externally generated gids`() {
        runBlocking {
            val conn = setupTestData()
            val ex = assertFailsWith<QBitException> {
                conn.persist(IntEntity(Gid(1, 81).value(), 0))
            }
            assertEquals("", ex.message)
        }
    }

    @Ignore
    @JsName("Peristance_of_entity_with_all_attr_eq_null_should_actually_persist_the_entity")
    @Test
    fun `Peristance of entity with all attr = null should actually persist the entity`() {
        runBlocking {
            val conn = setupTestSchema()
            val (stored) = conn.persist(NullableScalarWithoutPlaceholder(null, null))
            assertNotNull(stored)
            assertNotNull(conn.db().pull<NullableScalarWithoutPlaceholder>(stored.id!!))
            assertNull(conn.db().pull<NullableScalarWithoutPlaceholder>(stored.id!!)!!.scalar)
        }
    }

    @Ignore
    @JsName("Test_persistence_of_entity_without_attributes")
    @Test
    fun `Test persistence of entity without attributes`() {
        runBlocking {
            // Consider storing entities without attributes, to enable use case, when untyped attributes add later to the entity
            val conn = setupTestSchema()
            val (stored) = conn.persist(EntityWithoutAttrs(null))
            assertNotNull(stored)
            assertNotNull(conn.db().pull<EntityWithoutAttrs>(stored.id!!))
        }
    }

    @JsName("Reopening_existing_storage_should_preserve_state")
    @Test
    fun `Reopening existing storage should preserve state`() {
        runBlocking {
            val storage = MemStorage()
            setupTestSchema(storage)

            val conn1 = qbit(storage, testsSerialModule)
            assertNotNull((conn1.db() as InternalDb).attr(Scientists.name.name))
            conn1.persist(IntEntity(null, 2))

            val conn2 = qbit(storage, testsSerialModule)
            assertNotNull(conn2.db().query<IntEntity>(attrIs(IntEntities.int, 2)).firstOrNull())
        }
    }

    @JsName("Test_bomb_with_nulls_handling")
    @Test
    fun `Test bomb with nulls handling`() {
        runBlocking {
            val conn = setupTestData()
            val bomb = createBombWithNulls(Gid(2, 102).value())
            conn.persist(bomb)
            val storedBomb = conn.db().query<Bomb>(hasAttr(Bombs.bool), fetch = Eager).firstOrNull()
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
            assertNull(storedBomb.optStr)
            assertNull(storedBomb.strListOpt)
            assertNull(storedBomb.optBytes)
            assertNull(storedBomb.bytesListOpt)
            assertNull(storedBomb.optCountry)
            assertNull(storedBomb.countriesListOpt)
            assertNull(storedBomb.mutOptCountry)
            assertNull(storedBomb.mutCountriesListOpt)
        }
    }

    @JsName("Test_bomb_without_nulls_handling")
    @Test
    fun `Test bomb without nulls handling`() {
        runBlocking {
            val conn = setupTestData()
            val bomb = createBombWithoutNulls(Gid(2, 102).value())
            conn.persist(bomb)
            val storedBomb = conn.db().query<Bomb>(hasAttr(Bombs.bool), fetch = Eager).first()

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
            assertEquals(
                listOf(Country(12884901889, "Country1", 0), Country(4294967383, "Country3", 2)),
                storedBomb.countiesList
            )
            // todo: assertEquals(bomb.countriesListOpt, storedBomb.countriesListOpt)

            assertEquals(bomb.mutCountry, storedBomb.mutCountry)
            assertEquals(bomb.mutOptCountry, storedBomb.mutOptCountry)
            assertEquals(bomb.mutCountriesList, storedBomb.mutCountriesList)
            assertEquals(bomb.mutCountriesListOpt, storedBomb.mutCountriesListOpt)

            assertEquals(Gid(2, 102).value(), storedBomb.optBomb!!.id)
        }
    }

    @JsName("Test_conflict_resolving_in_no_conflicts_case_with_same_changes")
    @Test
    fun `Test conflict resolving in no conflicts case with same changes`() {
        runBlocking {
            val conn = setupTestData()
            val trx1 = conn.trx()
            trx1.persist(eCodd.copy(name = "Im same change"))
            val trx2 = conn.trx()
            trx2.persist(eCodd.copy(name = "Im same change"))
            trx1.commit()
            trx2.commit()
            conn.db {
                assertEquals("Im same change", it.pull<Scientist>(eCodd.id!!)!!.name)
            }
        }
    }

    @JsName("Test_conflict_resolving_in_no_conflicts_case_with_different_changes")
    @Test
    fun `Test conflict resolving in no conflicts case with different changes`() {
        runBlocking {
            val conn = setupTestData()
            val trx1 = conn.trx()
            trx1.persist(eCodd.copy(name = "Im different change"))
            val trx2 = conn.trx()
            trx2.persist(pChen.copy(name = "Im different change"))
            trx1.commit()
            trx2.commit()
            conn.db {
                assertEquals("Im different change", it.pull<Scientist>(eCodd.id!!)!!.name)
                assertEquals("Im different change", it.pull<Scientist>(pChen.id!!)!!.name)
            }
        }
    }

    @JsName("Test_conflict_resolving_in_conflicts_case")
    @Test
    fun `Test conflict resolving in conflicts case`() {
        runBlocking {
            val conn = setupTestData()
            val trx1 = conn.trx()
            trx1.persist(eCodd.copy(name = "Im change 1"))
            trx1.persist(eBrewer.copy(name = "Im different change"))
            val trx2 = conn.trx()
            trx2.persist(eCodd.copy(name = "Im change 2"))
            delay(100)
            trx2.persist(pChen.copy(name = "Im different change"))
            trx1.commit()
            trx2.commit()
            conn.db {
                assertEquals("Im change 2", it.pull<Scientist>(eCodd.id!!)!!.name)
                assertEquals("Im different change", it.pull<Scientist>(pChen.id!!)!!.name)
                assertEquals("Im different change", it.pull<Scientist>(eBrewer.id!!)!!.name)
            }
        }
    }

    @JsName("Test_merge_node_creating")
    @Test
    fun `Test merge node creating`() {
        runBlocking {
            val storage = MemStorage()
            val conn1 = setupTestData(storage)
            val trx1 = conn1.trx()
            val trx2 = conn1.trx()
            trx1.persist(eCodd.copy(name = "Im change 1"))
            trx1.persist(eBrewer.copy(name = "Im different change"))
            trx2.persist(eCodd.copy(name = "Im change 2"))
            trx2.persist(pChen.copy(name = "Im different change"))
            trx1.commit()
            trx2.commit()
            val trx3 = conn1.trx()
            trx3.persist(mStonebreaker.copy(name = "Im change 3"))
            trx3.commit()
            val conn2 = qbit(storage, testsSerialModule)
            conn2.db {
                assertEquals("Im change 2", it.pull<Scientist>(eCodd.id!!)!!.name)
                assertEquals("Im different change", it.pull<Scientist>(pChen.id!!)!!.name)
                assertEquals("Im different change", it.pull<Scientist>(eBrewer.id!!)!!.name)
                assertEquals("Im change 3", it.pull<Scientist>(mStonebreaker.id!!)!!.name)
            }
        }
    }

    @JsName("qbit_should_successfully_persist_and_load_numeric_attributes")
    @Test
    fun `qbit should successfully persist and load numeric attributes`(): Unit = runBlocking {
        // Given qbit and entity with numeric attribytes
        val storage = MemStorage()
        val conn1 = setupTestData(storage)
        val entity = EntityWithNullableNumericAttrs(null, 1, 2, 3)

        // When the entity is persisted
        val stored = conn1.persist(entity)
        // And storage is reopened
        val conn2 = qbit(storage, testsSerialModule)
        // And the entity is pulled
        val loaded = conn2.db().pull<EntityWithNullableNumericAttrs>(Gid(stored.persisted!!.id!!))!!

        // Then it should have persisted value attrs
        assertEquals(entity.long, loaded.long)
        assertEquals(entity.int, loaded.int)
        assertEquals(entity.byte, loaded.byte)
    }

    @JsName("Test_conflict_resolving_for_list_attr")
    @Test
    fun `Test conflict resolving for list attr`() {
        runBlocking {
            val conn = setupTestData()
            val trx1 = conn.trx()
            trx1.persist(
                eCodd.copy(
                    name = "Im change 1",
                    nicks = listOf("mathematician", "tabulator", "test1")
                )
            )
            val trx2 = conn.trx()
            trx2.persist(
                eCodd.copy(
                    name = "Im change 2",
                    nicks = listOf("mathematician", "tabulator", "test2")
                )
            )
            trx1.commit()
            trx2.commit()
            conn.db {
                assertEquals("Im change 2", it.pull<Scientist>(eCodd.id!!)!!.name)
                assertEquals(
                    listOf("mathematician", "tabulator", "test1", "test2"),
                    it.pull<Scientist>(eCodd.id!!)!!.nicks
                )
            }
        }
    }

    @JsName("Test_creating_entities_without_eid_in_parallel_transactions")
    @Test
    fun `Test creating entities without eid in parallel transactions`() {
        runBlocking {
            val storage = MemStorage()
            val conn = setupTestData(storage)
            val trx1 = conn.trx()
            val trx2 = conn.trx()
            val storedPaper = trx1.persist(Paper(null, "trx1", eCodd)).persisted
            val storedCity = trx2.persist(City(null,"trx2", nsk)).persisted
            trx1.commit()
            trx2.commit()
            val db = conn.db() as InternalDb
            val trx1EntityAttrValues = db.pullEntity(Gid(storedPaper!!.id!!))!!.entries
            assertEquals(2, trx1EntityAttrValues.size)
            assertEquals("trx1", trx1EntityAttrValues.first { it.attr.name == "Paper/name" }.value)
            assertEquals(Gid(eCodd.id!!), trx1EntityAttrValues.first { it.attr.name == "Paper/editor" }.value)
            val trx2EntityAttrValues = db.pullEntity(Gid(storedCity!!.id!!))!!.entries
            assertEquals(2, trx2EntityAttrValues.size)
            assertEquals("trx2", trx2EntityAttrValues.first { it.attr.name == "City/name" }.value)
            assertEquals(Gid(nsk.id!!), trx2EntityAttrValues.first { it.attr.name == "City/region" }.value)
        }
    }
}