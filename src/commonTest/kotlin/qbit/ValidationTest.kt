package qbit

import qbit.model.*
import qbit.ns.Namespace
import qbit.ns.root
import kotlin.test.Test
import kotlin.test.assertFailsWith


class ValidationTest {

    @Test
    fun testUniqueConstraintViolationWithinTrx() {
        assertFailsWith<QBitException> {
            val attr = Attr<String>("unique", true)
            val db = dbOf(EID(0, 0).nextEids(), attr)
            validate(db, listOf(Fact(EID(0, 0), attr, 0), Fact(EID(0, 1), attr, 0)))
        }
    }

    @Test
    fun testUnqiureAttrRestoring() {
        val attr = Attr<String>("unique", true)
        val db = dbOf(EID(0, 0).nextEids(), attr, Entity(attr eq "unique"))
        validate(db, listOf(Fact(EID(0, 1), attr, "unique")))
    }

    @Test
    fun testCreateAndUseAttr() {
        val attr = Attr<String>("unique", true)
        val db = dbOf(EID(0, 0).nextEids(), *bootstrapSchema.values.toTypedArray())
        validate(db, listOf(Fact(EID(0, 1), attr, "unique")), listOf(attr))
    }

    @Test
    fun testMultipleFactsForScalarAttr() {
        assertFailsWith<QBitException> {
            val attr = Attr<String>("scalar", true)
            val db = dbOf(EID(0, 0).nextEids(), attr)
            validate(db, listOf(Fact(EID(0, 1), attr, "scalar1"),
                    Fact(EID(0, 1), attr, "scalar2")))
        }
    }

}
