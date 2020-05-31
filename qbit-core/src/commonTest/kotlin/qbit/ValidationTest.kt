package qbit

import qbit.Scientists.extId
import qbit.api.QBitException
import qbit.api.firstInstanceEid
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Eav
import qbit.trx.validate
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFailsWith


class ValidationTest {


    @JsName("Addition_of_unique_attribute_value_with_different_gid_should_lead_to_QbitException")
    @Test
    fun `Addition of unique attribute value with different gid should lead to QbitException`() {
        assertFailsWith<QBitException> {
            val db = dbOf(Gid(0, 0).nextGids(), *(bootstrapSchema.values + testSchema).toTypedArray()).with(eCodd.toFacts())
            validate(db, listOf(Eav(Gid(pChen.id!!), extId, eCodd.externalId)))
        }
    }

    @JsName("Subsequent_storing_of_unique_value_for_the_same_entity_should_not_be_treated_as_uniqueness_violation")
    @Test
    fun `Subsequent storing of unique value for the same entity should not be treated as uniqueness violation`() {
        val db = dbOf(Gid(0, firstInstanceEid).nextGids(), *(bootstrapSchema.values + testSchema).toTypedArray()).with(eCodd.toFacts())
        validate(db, listOf(Eav(Gid(eCodd.id!!), extId, eCodd.externalId)))
    }

    @Test
    fun testCreateAndUseAttr() {
        val attr = Attr<String>("unique", true)
        val db = dbOf(Gid(0, 0).nextGids(), *bootstrapSchema.values.toTypedArray())
        validate(db, listOf(Eav(Gid(0, 1), attr, "unique")), listOf(attr))
    }

    @Test
    fun testMultipleFactsForScalarAttr() {
        assertFailsWith<QBitException> {
            val attr = Attr<String>("scalar", true)
            val db = dbOf(Gid(0, 0).nextGids(), attr)
            validate(db, listOf(Eav(Gid(0, 1), attr, "scalar1"),
                    Eav(Gid(0, 1), attr, "scalar2")))
        }
    }

}
