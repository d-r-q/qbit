package qbit

import qbit.Scientists.extId
import qbit.Scientists.name
import qbit.api.*
import qbit.api.db.attrIs
import qbit.api.db.pull
import qbit.api.db.query
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.model.impl.gid
import qbit.api.system.Instance
import qbit.factorization.destruct
import qbit.ns.Key
import qbit.ns.ns
import qbit.storage.MemStorage
import qbit.test.model.Scientist
import qbit.trx.QTrx
import kotlin.test.*


class TrxTest {

    @Test
    fun `Test transaction commit`() {
        val conn = setupTestData()
        val trx = conn.trx()
        trx.persist(eCodd.copy(name = "Not A Codd"))
        trx.commit()
        assertEquals("Not A Codd", conn.db().pull<Scientist>(eCodd.gid!!)!!.name)
    }

    @Test
    fun `Qbit should detect concurrent transactions`() {
        val conn = qbit(MemStorage())
        val trx1 = conn.trx()
        val trx2 = conn.trx()

        trx2.persist(extId)
        trx2.commit()

        try {
            trx1.persist(name)
            trx1.commit()
            fail()
        } catch (e: ConcurrentModificationException) {
            // expected
        }
        conn.db {
            assertNotNull(it.query<Attr<Any>>(attrIs(Attrs.name, extId.name)).firstOrNull())
            assertNull(it.query<Attr<Any>>(attrIs(Attrs.name, name.name)).firstOrNull())
        }
    }

    @Test
    fun `Qbit should ignore persistence of not changed entity`() {
        val trxLog = FakeTrxLog()
        val conn = FakeConn()
        val entities = arrayOf(Attrs.name, Attrs.type, Attrs.list, Attrs.unique,
                Instances.iid, Instances.nextEid, Instances.forks,
                extId)
        val trx = createTrx(conn, trxLog, *entities)
        trx.persist(extId)
        trx.commit()
        assertEquals(0, trxLog.appendsCalls)
        assertEquals(0, conn.updatesCalls)
    }

    @Test
    fun `When entity graph to store contains both updated and stored entities, only updated entity should be actually stored`() {
        val trxLog = FakeTrxLog()
        val conn = FakeConn()
        val entities = arrayOf(Attrs.name, Attrs.type, Attrs.list, Attrs.unique,
                Instances.iid, Instances.nextEid, Instances.forks,
                Countries.name, Countries.population,
                Regions.name, Regions.country,
                nsk)
        val trx = createTrx(conn, trxLog, *entities)
        trx.persist(nsk.copy(name = "Novonikolaevskaya obl."))
        trx.commit()
        assertEquals(1, trxLog.appendsCalls)
        assertEquals(1, conn.updatesCalls)
        assertEquals(5, trxLog.appendedFacts[0].size, "Five facts (2 for region and 3 for instance) expected")
        assertTrue(trxLog.appendedFacts[0].any { it.value == "Novonikolaevskaya obl." })
    }

    @Test
    fun `When entity graph to store contains both new and stored entities, only updated entity should be actually stored`() {
        val trxLog = FakeTrxLog()
        val conn = FakeConn()
        val entities = arrayOf(Attrs.name, Attrs.type, Attrs.list, Attrs.unique,
                Instances.iid, Instances.nextEid, Instances.forks,
                Countries.name, Countries.population,
                Regions.name, Regions.country,
                ru)
        val trx = createTrx(conn, trxLog, *entities)
        trx.persist(Region(null, "Kemerovskaya obl.", ru))
        trx.commit()
        assertEquals(1, trxLog.appendsCalls)
        assertEquals(1, conn.updatesCalls)
        assertEquals(5, trxLog.appendedFacts[0].size, "5 facts (2 for region and 3 for instance) expected")
        assertTrue(trxLog.appendedFacts[0].any { it.value == "Kemerovskaya obl." })
    }

    private fun createTrx(conn: FakeConn, trxLog: FakeTrxLog, vararg entities: Any) =
            QTrx(Instance(Gid(0, 0), 0, 0, 0), trxLog, dbOf(Gid(0, 0).nextGids(),
                    *entities), conn, ::destruct)

    @Ignore
    @Test
    fun `In case of transaction commit abort, transaction should clean up written data`() {
        val storage = MemStorage()
        val conn = qbit(storage)
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

    @Test
    fun `Test rollback`() {
        val conn = setupTestData()
        val trx = conn.trx()
        trx.persist(eCodd.copy(name = "Not A Codd"))
        assertEquals("Not A Codd", trx.db().pull<Scientist>(eCodd.gid!!)!!.name)
        trx.rollback()
        assertEquals("Edgar Codd", trx.db().pull<Scientist>(eCodd.gid!!)!!.name)
    }

    @Test
    fun `Commit of rolled back transaction should fail`() {
        val storage = MemStorage()
        val conn = qbit(storage)
        val trx = conn.trx()
        trx.rollback()
        val ex = assertFailsWith<QBitException> {
            trx.commit()
        }
        assertEquals("Transaction already has been rollbacked", ex.message)
    }

    @Test
    fun `Persist with rolled back transaction should fail`() {
        val storage = MemStorage()
        val conn = qbit(storage)
        val trx = conn.trx()
        trx.rollback()
        val ex = assertFailsWith<QBitException> {
            trx.persist(Any())
        }
        assertEquals("Transaction already has been rollbacked", ex.message)
    }
}