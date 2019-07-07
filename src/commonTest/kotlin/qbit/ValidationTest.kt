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
            val attr = ScalarAttr(root["unique"], QString, true)
            val db = dbOf(attr)
            validate(db, listOf(Fact(EID(0, 0), attr, 0), Fact(EID(0, 1), attr, 0)))
        }
    }

    @Test
    fun testUnqiureAttrRestoring() {
        val attr = ScalarAttr(root["unique"], QString, true)
        val db = dbOf(attr, Entity(attr eq "unique"))
        validate(db, listOf(Fact(EID(0, 1), attr, "unique")))
    }

    @Test
    fun testCreateAndUseAttr() {
        val attr = ScalarAttr(root["unique"], QString, true)
        val db = dbOf(ScalarAttr(Namespace("qbit").subNs("attr")["name"], QString),
                ScalarAttr(Namespace("qbit").subNs("attr")["type"], QByte),
                ScalarAttr(Namespace("qbit").subNs("attr")["unique"], QBoolean),
                ScalarAttr(Namespace("qbit").subNs("attr")["list"], QBoolean))
        validate(db, listOf(Fact(EID(0, 1), attr, "unique")), listOf(attr))
    }

    @Test
    fun testMultipleFactsForScalarAttr() {
        assertFailsWith<QBitException> {
            val attr = ScalarAttr(root["scalar"], QString, true)
            val db = dbOf(attr)
            validate(db, listOf(Fact(EID(0, 1), attr, "scalar1"),
                    Fact(EID(0, 1), attr, "scalar2")))
        }
    }
}
