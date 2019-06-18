package qbit

import org.junit.Assert.*
import org.junit.Test
import qbit.ns.root
import qbit.schema.ScalarAttr
import qbit.storage.MemStorage


class TrxTest {

    @Test
    fun testConcurrentModificationDetection() {
        val conn = qbit(MemStorage())
        val trx = conn.trx()
        conn.persist(ScalarAttr(root["any"], QString))
        try {
            trx.persist(ScalarAttr(root["should not be persisted"], QString))
            fail()
        } catch (e: ConcurrentModificationException) {
            // expected
        }
        assertNotNull(conn.db.query(attrIs(EAttr.name, "/any")).firstOrNull())
        assertNull(conn.db.query(attrIs(EAttr.name, "/should not be persisted")).firstOrNull())
    }
}