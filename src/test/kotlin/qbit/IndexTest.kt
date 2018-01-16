package qbit

import org.junit.Assert.assertEquals
import org.junit.Test

class IndexTest {

    @Test
    fun testVaet() {
        val f1 = StoredFact(EID(1, 0), "attr2", 1, "value2")
        assertEquals(0, vaetCmp(f1, f1))

        val byValue = FactPattern(null, null, null, "value2")
        assertEquals(0, vaetCmp(f1, byValue))
        assertEquals(0, vaetCmp(byValue, f1))

        val lesserValue = FactPattern(null, "attr2", 1, "value1")
        assertEquals(1, vaetCmp(f1, lesserValue))
        assertEquals(-1, vaetCmp(lesserValue, f1))

        val greaterValue = FactPattern(null, "attr2", 1, "value3")
        assertEquals(-1, vaetCmp(f1, greaterValue))
        assertEquals(1, vaetCmp(greaterValue, f1))

        val lesserAttr = FactPattern(null, "attr1", 1, "value2")
        assertEquals(1, vaetCmp(f1, lesserAttr))
        assertEquals(-1, vaetCmp(lesserAttr, f1))

        val greaterAttr = FactPattern(null, "attr3", 1, "value2")
        assertEquals(-1, vaetCmp(f1, greaterAttr))
        assertEquals(1, vaetCmp(greaterAttr, f1))

        val lesserEid = FactPattern(EID(0, 0), "attr2", 1, "value2")
        assertEquals(1, vaetCmp(f1, lesserEid))
        assertEquals(-1, vaetCmp(lesserEid, f1))

        val greaterEid = FactPattern(EID(2, 0), "attr2", 1, "value2")
        assertEquals(-1, vaetCmp(f1, greaterEid))
        assertEquals(1, vaetCmp(greaterEid, f1))

        val lesserTime = FactPattern(EID(1, 0), "attr2", 0, "value2")
        assertEquals(1, vaetCmp(f1, lesserTime))
        assertEquals(-1, vaetCmp(lesserTime, f1))

        val greaterTime = FactPattern(EID(1, 0), "attr2", 3, "value2")
        assertEquals(-1, vaetCmp(f1, greaterTime))
        assertEquals(1, vaetCmp(greaterTime, f1))
    }

}