package qbit

import org.junit.Test
import qbit.model.*
import qbit.ns.root
import qbit.util.QBitException

class ValidationTest {

    @Test(expected = QBitException::class)
    fun testUniqueConstraintViolationWithinTrx() {
        val attr = ScalarAttr(root["unique"], DataType.QString, true)
        val db = dbOf(attr)
        validate(db, listOf(Fact(EID(0, 0), attr, 0), Fact(EID(0, 1), attr, 0)))
    }

    @Test
    fun testUnqiureAttrRestoring() {
        val attr = ScalarAttr(root["unique"], DataType.QString, true)
        val db = dbOf(attr, Entity(attr eq "unique"))
        validate(db, listOf(Fact(EID(0, 1), attr, "unique")))
    }
}
