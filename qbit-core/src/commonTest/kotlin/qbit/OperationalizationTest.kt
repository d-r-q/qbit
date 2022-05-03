package qbit

import qbit.api.gid.nextGids
import qbit.factoring.serializatoin.KSFactorizer
import qbit.test.model.IntCounterEntity
import qbit.trx.operationalize
import qbit.typing.qbitCoreTestsSerialModule
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class OperationalizationTest {

    private val gids = qbit.api.gid.Gid(0, 0).nextGids()

    val factor = KSFactorizer(qbitCoreTestsSerialModule)::factor

    val emptyDb = dbOf(gids, *(bootstrapSchema.values + testSchema).toTypedArray())

    @JsName("Counter_not_persisted_in_db_should_pass_as_is")
    @Test
    fun `Counter not persisted in db should pass as-is`() {
        val counterEntity = IntCounterEntity(null, 10)
        val facts = operationalize(emptyDb, factor(counterEntity, emptyDb::attr, gids).entityFacts.values.first())
        assertEquals(1, facts.size, "Factoring of single entity with single attr should produce single fact")
        assertEquals("IntCounterEntity/counter", facts[0].attr)
        assertEquals(10, facts[0].value)
    }

    @JsName("Persisted_counter_should_turn_into_difference")
    @Test
    fun `Persisted counter should turn into difference`() {
        val counterEntity = IntCounterEntity(1, 10)
        val updatedDb = emptyDb.with(factor(IntCounterEntity(1, 10), emptyDb::attr, gids))

        val facts = operationalize(updatedDb, factor(counterEntity.copy(counter = 100), updatedDb::attr, gids).entityFacts.values.first())
        assertEquals(1, facts.size, "Factoring of single entity with single attr should produce single fact")
        assertEquals("IntCounterEntity/counter", facts[0].attr)
        assertEquals(90, facts[0].value)
    }
}