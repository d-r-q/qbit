package qbit

import qbit.model.Attr2
import qbit.model.IID
import qbit.storage.MemStorage
import qbit.trx.Instance
import qbit.trx.bootstrap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class BootstrapTest {

    private val newDb = bootstrap(DbUuid(IID(1, 4)), MemStorage())

    @Test
    fun `Attr attrs is correctly bootstrapped`() {
        assertEquals(Attrs.name.name, newDb.db().queryT<Attr2<*>>(attrIs(Attrs.name, Attrs.name.name)).first().name)
        assertEquals(Attrs.type.name, newDb.db().queryT<Attr2<*>>(attrIs(Attrs.name, Attrs.type.name)).first().name)
        assertEquals(Attrs.unique.name, newDb.db().queryT<Attr2<*>>(attrIs(Attrs.name, Attrs.unique.name)).first().name)
        assertEquals(Attrs.list.name, newDb.db().queryT<Attr2<*>>(attrIs(Attrs.name, Attrs.list.name)).first().name)
    }

    @Test
    fun `Instance attrs is correctly bootstrapped`() {
        assertEquals(Instances.iid.name, newDb.db().queryT<Attr2<*>>(attrIs(Attrs.name, Instances.iid.name)).first().name)
        assertEquals(Instances.forks.name, newDb.db().queryT<Attr2<*>>(attrIs(Attrs.name, Instances.forks.name)).first().name)
        assertEquals(Instances.nextEid.name, newDb.db().queryT<Attr2<*>>(attrIs(Attrs.name, Instances.nextEid.name)).first().name)
    }

    @Test
    fun `Tombstone attr is correctly bootstrapped`() {
        assertEquals(tombstone.name, newDb.db().queryT<Attr2<*>>(attrIs(Attrs.name, tombstone.name)).first().name)
    }

    @Test
    fun `The Instance's next eid value is correctly bootstrapped`() {
        val theInstance = newDb.db().queryT<Instance>(attrIs(Instances.iid, 1)).first()
        val actualMaxEidValue = newDb.db().queryGids().map { it.eid }.max()!!
        assertTrue(theInstance.nextEid > actualMaxEidValue, "Next eid value ${theInstance.nextEid} should be greater, than actual max eid value $actualMaxEidValue")
    }

}