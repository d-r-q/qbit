package qbit.model

import qbit.Entity
import qbit.AttachedEntity
import qbit.Attr
import qbit.ListAttr
import qbit.api.model.StoredEntity
import qbit.api.model.Attr
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.QInt
import qbit.api.model.QRef
import qbit.assertArrayEquals
import qbit.ns.Namespace
import qbit.trx.toFacts
import kotlin.test.*

@Suppress("UNCHECKED_CAST")
class EntityTest {

    @Test
    fun testCreate() {
        val gids = Gid(0, 0).nextGids()
        val user = Namespace("user")
        val _attr = Attr<String>(null, user["attr"].name)
        val _list = ListAttr<String, List<String>>(null, user["list"].name)
        val _ref = Attr<Any>(null, user["ref"].name)
        val _refList = ListAttr<Gid, List<Gid>>(null, (user["refList"]).name)
        val _eid = Attr<Gid>(null, user["some_eid"].name)

        val e1 = Entity(gids.next(), _attr eq "e1")
        val e2 = Entity(
                gids.next(),
                _attr eq "e2",
                _ref eq e1,
                _eid eq Gid(0, 3),
                _list eq listOf("one", "two"),
                _refList eq listOf(e1))
        assertTrue((e2 as DetachedEntity).entries.size == 5)

        assertEquals("e2", e2[_attr])
        assertEquals(Gid(0, 3), e2[_eid])
        assertEquals(e2[_ref], e1.gid)
        assertArrayEquals(arrayOf("one", "two"), e2[_list].toTypedArray())
        val list: List<Gid> = e2[_refList]
        assertArrayEquals(arrayOf(e1.gid), list.toTypedArray())
        assertEquals(5, e2.entries.size)
    }

    @Test
    fun `Getting value of reference attr should return gid`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()
        val referred = AttachedEntity(gids.next(), mapOf<Attr<Any>, Any>(Attr<Int>("any") to 0), map::get)
        val refAttr = Attr<Gid>("ref")
        val referring = AttachedEntity(gids.next(), mapOf<Attr<Any>, Any>(refAttr to referred), map::get)
        map[referred.gid] = referred
        assertEquals(referred.gid, referring[refAttr])
    }

    @Test
    fun `Pulling value of reference attr should return StoredEntity`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()
        val referred = AttachedEntity(gids.next(), mapOf<Attr<Any>, Any>(Attr<Int>("any") to 0), map::get)
        val refAttr = Attr<Gid>("ref")
        val referring = AttachedEntity(gids.next(), mapOf<Attr<Any>, Any>(refAttr to referred), map::get)
        map[referred.gid] = referred
        assertEquals(referred, referring.pull(referred.gid))
    }

    @Ignore
    @Test
    fun eqIsTypeSave() {
        // Try to find out of type safe way to entity creation
        val attr = Attr<Int>(null, "", QInt.code, unique = false, list = false)
        val entries = attr eq ""
        Entity(Gid(0, 0), entries)
        fail("attr eq \"\" should not compile")
    }

    @Test
    fun `Factorization of reference attribute returns Fact(_, _, value = gid)`() {
        val e1 = Entity(Gid(0, 0))
        val e2 = Entity(Gid(0, 1), Attr<Any>("ref") eq e1)
        val value = e2.toFacts().first().value
        assertTrue(value is Gid, "Gid expected, but got $value")
    }

    @Test
    fun `Creation of Attr for Any class has QRef type`() {
        assertEquals(QRef.code, Attr<Any>("any").type)
    }

    @Test
    fun `Test Tombstone toString`() {
        assertEquals("Tombstone(gid = 0/0)", QTombstone(Gid(0, 0)).toString())
    }

}