package qbit

import kotlinx.serialization.modules.plus
import qbit.api.Attrs
import qbit.api.Instances
import qbit.api.db.Conn
import qbit.api.db.attrIs
import qbit.api.db.query
import qbit.api.gid.Iid
import qbit.api.model.Attr
import qbit.api.system.DbUuid
import qbit.api.system.Instance
import qbit.ns.Namespace
import qbit.platform.runBlocking
import qbit.storage.MemStorage
import qbit.test.model.testsSerialModule
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class BootstrapTest {

    private val storage = MemStorage()

    private suspend fun newDb(): Conn = bootstrap(
        storage,
        DbUuid(Iid(1, 4)),
        testSchemaFactorizer::factor,
        qbitSerialModule + testsSerialModule
    )

    @Test
    fun testInit() {
        runBlocking {
            val db = qbit(storage, testsSerialModule)
            assertNotNull(db)
            assertTrue(storage.keys(Namespace("nodes")).isNotEmpty())
        }
    }

    @JsName("Attr_attrs_is_correctly_bootstrapped")
    @Test
    fun `Attr attrs is correctly bootstrapped`() {
        runBlocking {
            val db = newDb()
            assertEquals(Attrs.name.name, db.db().query<Attr<*>>(attrIs(Attrs.name, Attrs.name.name)).first().name)
            assertEquals(Attrs.type.name, db.db().query<Attr<*>>(attrIs(Attrs.name, Attrs.type.name)).first().name)
            assertEquals(
                Attrs.unique.name,
                db.db().query<Attr<*>>(attrIs(Attrs.name, Attrs.unique.name)).first().name
            )
            assertEquals(Attrs.list.name, db.db().query<Attr<*>>(attrIs(Attrs.name, Attrs.list.name)).first().name)
        }
    }

    @JsName("Instance_attrs_is_correctly_bootstrapped")
    @Test
    fun `Instance attrs is correctly bootstrapped`() {
        runBlocking {
            val db = newDb()
            assertEquals( Instances.iid.name, db.db().query<Attr<*>>(attrIs(Attrs.name, Instances.iid.name)).first().name )
            assertEquals( Instances.forks.name, db.db().query<Attr<*>>(attrIs(Attrs.name, Instances.forks.name)).first().name )
            assertEquals( Instances.nextEid.name, db.db().query<Attr<*>>(attrIs(Attrs.name, Instances.nextEid.name)).first().name )
        }
    }

    @JsName("Tombstone_attr_is_correctly_bootstrapped")
    @Test
    fun `Tombstone attr is correctly bootstrapped`() {
        runBlocking {
            assertEquals(
                qbit.api.tombstone.name,
                newDb().db().query<Attr<*>>(attrIs(Attrs.name, qbit.api.tombstone.name)).first().name
            )
        }
    }

    @JsName("The_Instances_next_eid_value_is_correctly_bootstrapped")
    @Test
    fun `The Instance's next eid value is correctly bootstrapped`() {
        runBlocking {
            val db = newDb().db()
            val theInstance = db.query<Instance>(attrIs(Instances.iid, 1)).first()
            val actualMaxEidValue = db.queryGids().map { it.eid }.maxOrNull()!!
            assertTrue(
                theInstance.nextEid > actualMaxEidValue,
                "Next eid value ${theInstance.nextEid} should be greater, than actual max eid value $actualMaxEidValue"
            )
        }
    }

}