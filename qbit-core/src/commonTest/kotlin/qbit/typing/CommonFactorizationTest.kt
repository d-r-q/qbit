package qbit.typing

import kotlinx.serialization.Serializable
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.api.model.QString
import qbit.factorization.Destruct
import kotlin.js.JsName
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


@Serializable
data class Addr(val id: Long?, val addr: String)

abstract class CommonFactorizationTest(val destruct: Destruct) {

    private val gids = Gid(0, 0).nextGids()

    private val testSchema = mapOf(
        ".qbit.typing.Addr/addr" to Attr<String>(
            gids.next(),
            ".qbit.typing.Addr/addr",
            QString.code,
            false,
            false
        )
    )

    @Ignore
    @JsName("testSimpleEntityFactorization")
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

}