package qbit

import qbit.model.*
import kotlin.test.Test
import kotlin.test.assertFailsWith


class ValidationTest {

    @Test
    fun testUniqueConstraintViolationWithinTrx() {
        assertFailsWith<QBitException> {
            val attr = Attr<String>("unique", true)
            val db = dbOf(Gid(0, 0).nextEids(), attr)
            validate(db, listOf(Fact(Gid(0, 0), attr, 0), Fact(Gid(0, 1), attr, 0)))
        }
    }

    @Test
    fun testUnqiureAttrRestoring() {
        val attr = Attr<String>("unique", true)
        val db = dbOf(Gid(0, 0).nextEids(), attr, Entity(attr eq "unique"))
        validate(db, listOf(Fact(Gid(0, 1), attr, "unique")))
    }

    @Test
    fun testCreateAndUseAttr() {
        val attr = Attr<String>("unique", true)
        val db = dbOf(Gid(0, 0).nextEids(), *bootstrapSchema.values.toTypedArray())
        validate(db, listOf(Fact(Gid(0, 1), attr, "unique")), listOf(attr))
    }

    @Test
    fun testMultipleFactsForScalarAttr() {
        assertFailsWith<QBitException> {
            val attr = Attr<String>("scalar", true)
            val db = dbOf(Gid(0, 0).nextEids(), attr)
            validate(db, listOf(Fact(Gid(0, 1), attr, "scalar1"),
                    Fact(Gid(0, 1), attr, "scalar2")))
        }
    }

}
