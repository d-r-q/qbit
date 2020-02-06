package qbit.typing

import kotlinx.serialization.Serializable
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.api.model.QRef
import qbit.api.model.QString
import qbit.factorization.Destruct
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals


@Serializable
data class Addr(val id: Long?, val addr: String)

@Serializable
data class UserWithAddr(
    val id: Long? = null,
    val addr: Addr
)

abstract class CommonFactorizationTest(val destruct: Destruct) {

    private val gids = Gid(0, 0).nextGids()

    private val testSchema = mapOf(
        ".qbit.typing.Addr/addr" to Attr<String>(
            gids.next(),
            ".qbit.typing.Addr/addr",
            QString.code,
            false,
            false
        ),


        ".qbit.typing.UserWithAddr/addr" to Attr<Addr>(
            gids.next(),
            ".qbit.typing.UserWithAddr/addr",
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
        assertEquals(".qbit.typing.Addr/addr", facts[0].attr)
        assertEquals("addrValue", facts[0].value)
    }

    @JsName("Test_simple_peristed_entity_factorization")
    @Test
    fun `Test simple persisted entity factorization`() {
        val addr = Addr(1, "addrValue")

        val factorization = destruct(addr, testSchema::get, gids)
        assertEquals(1, factorization.size, "Factorization of single entity should produce facts for one entity")
        val facts = factorization.entityFacts.values.first()
        assertEquals(1, facts.size, "Factorization of single entity with single attr should produce single fact")
        assertEquals(Gid(1), facts[0].gid)
        assertEquals(".qbit.typing.Addr/addr", facts[0].attr)
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
        assertEquals(".qbit.typing.UserWithAddr/addr", facts[0].attr)
        assertEquals(Gid(2), facts[0].value)
        assertEquals(".qbit.typing.Addr/addr", facts[1].attr)
        assertEquals("addrValue", facts[1].value)
    }
}