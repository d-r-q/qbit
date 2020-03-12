package qbit

import qbit.api.Attrs
import qbit.api.Instances
import qbit.api.db.attrIs
import qbit.api.db.query
import qbit.api.gid.Iid
import qbit.api.model.Attr
import qbit.api.system.DbUuid
import qbit.api.system.Instance
import qbit.ns.Namespace
import qbit.storage.MemStorage
import qbit.test.model.testsSerialModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class BootstrapTest {

    private val storage = MemStorage()
    private val newDb = bootstrap(storage, DbUuid(Iid(1, 4)), testSchemaFactorization::ksDestruct)

    @Test
    fun testInit() {
        val db = qbit(storage, testsSerialModule)
        assertNotNull(db)
        assertTrue(storage.keys(Namespace("nodes")).isNotEmpty())
    }

    @Test
    fun `Attr attrs is correctly bootstrapped`() {
        assertEquals(Attrs.name.name, newDb.db().query<Attr<*>>(attrIs(Attrs.name, Attrs.name.name)).first().name)
        assertEquals(Attrs.type.name, newDb.db().query<Attr<*>>(attrIs(Attrs.name, Attrs.type.name)).first().name)
        assertEquals(Attrs.unique.name, newDb.db().query<Attr<*>>(attrIs(Attrs.name, Attrs.unique.name)).first().name)
        assertEquals(Attrs.list.name, newDb.db().query<Attr<*>>(attrIs(Attrs.name, Attrs.list.name)).first().name)
    }

    @Test
    fun `Instance attrs is correctly bootstrapped`() {
        assertEquals(Instances.iid.name, newDb.db().query<Attr<*>>(attrIs(Attrs.name, Instances.iid.name)).first().name)
        assertEquals(Instances.forks.name, newDb.db().query<Attr<*>>(attrIs(Attrs.name, Instances.forks.name)).first().name)
        assertEquals(Instances.nextEid.name, newDb.db().query<Attr<*>>(attrIs(Attrs.name, Instances.nextEid.name)).first().name)
    }

    @Test
    fun `Tombstone attr is correctly bootstrapped`() {
        assertEquals(qbit.api.tombstone.name, newDb.db().query<Attr<*>>(attrIs(Attrs.name, qbit.api.tombstone.name)).first().name)
    }

    @Test
    fun `The Instance's next eid value is correctly bootstrapped`() {
        val theInstance = newDb.db().query<Instance>(attrIs(Instances.iid, 1)).first()
        val actualMaxEidValue = newDb.db().queryGids().map { it.eid }.max()!!
        assertTrue(theInstance.nextEid > actualMaxEidValue, "Next eid value ${theInstance.nextEid} should be greater, than actual max eid value $actualMaxEidValue")
    }

}