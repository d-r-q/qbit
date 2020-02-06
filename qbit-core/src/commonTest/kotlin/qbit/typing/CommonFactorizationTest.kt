package qbit.typing

import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.api.model.QRef
import qbit.api.model.QString
import qbit.factorization.Destruct
import qbit.factorization.attrName
import qbit.test.model.Addr
import qbit.test.model.UserWithAddr
import kotlin.js.JsName
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class CommonFactorizationTest(val destruct: Destruct) {

    private var gids = Gid(0, 0).nextGids()

    @BeforeTest
    fun setUp() {
        gids = Gid(0, 1).nextGids()
    }

    private val testSchema = mapOf(
        ".qbit.test.model.Addr/addr" to Attr<String>(
            gids.next(),
            ".qbit.test.model.Addr/addr",
            QString.code,
            false,
            false
        ),


        ".qbit.test.model.UserWithAddr/addr" to Attr<Addr>(
            gids.next(),
            ".qbit.test.model.UserWithAddr/addr",
            QRef.code,
            false,
            false
        )
    )

    @JsName("Test_simple_entity_factorization")
    @Test
    fun `Test simple entity factorization`() {
        val addr = Addr(null, "addrValue")

        val factorization = destruct(addr, testSchema::get, gids)
        assertEquals(1, factorization.size, "Factorization of single entity should produce facts for one entity")
        val facts = factorization.entityFacts.values.first()
        assertEquals(1, facts.size, "Factorization of single entity with single attr should produce single fact")
        assertEquals(".qbit.test.model.Addr/addr", facts[0].attr)
        assertEquals("addrValue", facts[0].value)
    }

    @JsName("Test_simple_persisted_entity_factorization")
    @Test
    fun `Test simple persisted entity factorization`() {
        val addr = Addr(1, "addrValue")

        val factorization = destruct(addr, testSchema::get, gids)
        assertEquals(1, factorization.size, "Factorization of single entity should produce facts for one entity")
        val facts = factorization.entityFacts.values.first()
        assertEquals(1, facts.size, "Factorization of single entity with single attr should produce single fact")
        assertEquals(Gid(1), facts[0].gid)
        assertEquals(".qbit.test.model.Addr/addr", facts[0].attr)
        assertEquals("addrValue", facts[0].value)
    }

    @JsName("Test_entity_with_ref_factorization")
    @Test
    fun `Test entity with ref factorization`() {
        val user = UserWithAddr(null, Addr(null, "addrValue"))

        val factorization = destruct(user, testSchema::get, gids)
        assertEquals(2, factorization.size, "Factorization of entity with ref should produce facts for both entities")
        val facts = factorization.entityFacts.values.first()
        assertEquals(2, facts.size, "Factorization of two entities with single attr should produce two facts")
        assertEquals(".qbit.test.model.UserWithAddr/addr", facts[0].attr)
        assertEquals(Gid(1), facts[0].value)
        assertEquals(".qbit.test.model.Addr/addr", facts[1].attr)
        assertEquals("addrValue", facts[1].value)
    }

    @JsName("Test_SerialDescriptor_to_attr_name_conversion")
    @Test
    fun `Test SerialDescriptor to attr name conversion`() {
        assertEquals(".qbit.test.model.Addr/addr", attrName(Addr.serializer().descriptor, 1))
    }

}