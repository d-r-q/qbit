package qbit

import qbit.Users.extId
import qbit.Users.name
import qbit.ns.Key
import qbit.ns.ns
import qbit.storage.MemStorage
import qbit.trx.qbit
import kotlin.test.*


class TrxTest {

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
            assertNotNull(it.query(attrIs(Attrs.name, extId.name)).firstOrNull())
            assertNull(it.query(attrIs(Attrs.name, name.name)).firstOrNull())
        }
    }

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
}