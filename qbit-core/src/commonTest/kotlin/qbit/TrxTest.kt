package qbit

import qbit.Scientists.extId
import qbit.Scientists.name
import qbit.api.Attrs
import qbit.api.Instances
import qbit.api.QBitException
import qbit.api.db.Conn
import qbit.api.db.attrIs
import qbit.api.db.pull
import qbit.api.db.query
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.api.system.Instance
import qbit.ns.Key
import qbit.ns.ns
import qbit.platform.runBlocking
import qbit.spi.Storage
import qbit.storage.MemStorage
import qbit.test.model.Region
import qbit.test.model.Scientist
import qbit.test.model.testsSerialModule
import qbit.trx.QTrx
import kotlin.js.JsName
import kotlin.test.*


class TrxTest {

    @JsName("Test_transaction_commit")
    @Test
    fun `Test transaction commit`() {
        runBlocking {
            val conn = setupTestData()
            val trx = conn.trx()
            trx.persist(eCodd.copy(name = "Not A Codd"))
            trx.commit()
            assertEquals("Not A Codd", conn.db().pull<Scientist>(eCodd.id!!)!!.name)
        }
    }

    @JsName("Qbit_should_ignore_persistence_of_not_changed_entity")
    @Test
    fun `Qbit should ignore persistence of not changed entity`() {
        runBlocking {
            val trxLog = FakeTrxLog()
            val conn = FakeConn()
            val entities = arrayOf(
                Attrs.name, Attrs.type, Attrs.list, Attrs.unique,
                Instances.iid, Instances.nextEid, Instances.forks,
                extId
            )
            val trx = createTrx(conn, trxLog, *entities)
            trx.persist(extId)
            trx.commit()
            assertEquals(0, trxLog.appendsCalls)
            assertEquals(0, conn.updatesCalls)
        }
    }

    @JsName("When_entity_graph_to_store_contains_both_updated_and_stored_entities_only_updated_entity_should_be_actually_stored")
    @Test
    fun `When entity graph to store contains both updated and stored entities, only updated entity should be actually stored`() {
        runBlocking {
            val trxLog = FakeTrxLog()
            val conn = FakeConn()
            val entities = arrayOf(
                Attrs.name, Attrs.type, Attrs.list, Attrs.unique,
                Instances.iid, Instances.nextEid, Instances.forks,
                Countries.name, Countries.population,
                Regions.name, Regions.country,
                nsk
            )
            val trx = createTrx(conn, trxLog, *entities)
            trx.persist(nsk.copy(name = "Novonikolaevskaya obl."))
            trx.commit()
            assertEquals(1, trxLog.appendsCalls)
            assertEquals(1, conn.updatesCalls)
            assertEquals(5, trxLog.appendedFacts[0].size, "Five facts (2 for region and 3 for instance) expected")
            assertTrue(trxLog.appendedFacts[0].any { it.value == "Novonikolaevskaya obl." })
        }
    }

    @JsName("When_entity_graph_to_store_contains_both_new_and_stored_entities_only_updated_entity_should_be_actually_stored")
    @Test
    fun `When entity graph to store contains both new and stored entities, only updated entity should be actually stored`() {
        runBlocking {
            val trxLog = FakeTrxLog()
            val conn = FakeConn()
            val entities = arrayOf(
                Attrs.name, Attrs.type, Attrs.list, Attrs.unique,
                Instances.iid, Instances.nextEid, Instances.forks,
                Countries.name, Countries.population,
                Regions.name, Regions.country,
                ru
            )
            val trx = createTrx(conn, trxLog, *entities)
            trx.persist(Region(null, "Kemerovskaya obl.", ru))
            trx.commit()
            assertEquals(1, trxLog.appendsCalls)
            assertEquals(1, conn.updatesCalls)
            assertEquals(5, trxLog.appendedFacts[0].size, "5 facts (2 for region and 3 for instance) expected")
            assertTrue(trxLog.appendedFacts[0].any { it.value == "Kemerovskaya obl." })
        }
    }

    private fun createTrx(conn: FakeConn, trxLog: FakeTrxLog, vararg entities: Any) =
        QTrx(
            Instance(Gid(0, 1), 0, 0, 2), trxLog, dbOf(
                Gid(0, 0).nextGids(),
                *entities
            ), conn, testSchemaFactorizer::factor,
            GidSequence(Instance(Gid(0, 1), 0, 0, 2))
        )

    @Ignore
    @JsName("In_case_of_transaction_commit_abort_transaction_should_clean_up_written_data")
    @Test
    fun `In case of transaction commit abort, transaction should clean up written data`() {
        runBlocking {
            val (conn, storage) = openEmptyConn()
            val trx = conn.trx()
            conn.trx().persist(extId)
            try {
                trx.persist(name)
                trx.commit()
                fail()
            } catch (e: ConcurrentModificationException) {
                // expected
                val abortedTrxKey = Key(ns("node"), "e37c77c73f7b63afb8ffaadc065dadde6e140f")
                storage.hasKey(abortedTrxKey)
            }
        }
    }

    @JsName("Test_rollback")
    @Test
    fun `Test rollback`() {
        runBlocking {
            val conn = setupTestData()
            val trx = conn.trx()
            trx.persist(eCodd.copy(name = "Not A Codd"))
            assertEquals("Not A Codd", trx.db().pull<Scientist>(eCodd.id!!)!!.name)
            trx.rollback()
            assertEquals("Edgar Codd", trx.db().pull<Scientist>(eCodd.id!!)!!.name)
        }
    }

    @JsName("Commit_of_rolled_back_transaction_should_fail")
    @Test
    fun `Commit of rolled back transaction should fail`() {
        runBlocking {
            val (conn, _) = openEmptyConn()
            val trx = conn.trx()
            trx.rollback()
            val ex = assertFailsWith<QBitException> {
                trx.commit()
            }
            assertEquals("Transaction already has been rollbacked", ex.message)
        }
    }

    @JsName("Persist_with_rolled_back_transaction_should_fail")
    @Test
    fun `Persist with rolled back transaction should fail`() {
        runBlocking {
            val (conn, _) = openEmptyConn()
            val trx = conn.trx()
            trx.rollback()
            val ex = assertFailsWith<QBitException> {
                trx.persist(Any())
            }
            assertEquals("Transaction already has been rollbacked", ex.message)
        }
    }

    private suspend fun openEmptyConn(): Pair<Conn, Storage> {
        val storage = MemStorage()
        val conn = qbit(storage, testsSerialModule)
        return conn to storage
    }

}