package qbit

import org.junit.Assert.assertEquals
import org.junit.Test

class IndexTest {

    @Test
    fun testVaet() {
        val f1 = StoredFact(EID(1, 0), "attr2", 1, "value2")
        assertEquals(0, avetCmp(f1, f1))

        val byAttr = FactPattern(null, "attr2", null, null)
        assertEquals(0, avetCmp(f1, byAttr))
        assertEquals(0, avetCmp(byAttr, f1))

        val lesserAttr = FactPattern(null, "attr1", null, null)
        assertEquals(1, avetCmp(f1, lesserAttr))
        assertEquals(-1, avetCmp(lesserAttr, f1))

        val greaterAttr = FactPattern(null, "attr3", null, null)
        assertEquals(-1, avetCmp(f1, greaterAttr))
        assertEquals(1, avetCmp(greaterAttr, f1))

        val lesserValue = FactPattern(null, "attr2", null, "value1")
        assertEquals(1, avetCmp(f1, lesserValue))
        assertEquals(-1, avetCmp(lesserValue, f1))

        val greaterValue = FactPattern(null, "attr2", null, "value3")
        assertEquals(-1, avetCmp(f1, greaterValue))
        assertEquals(1, avetCmp(greaterValue, f1))

        val lesserEid = FactPattern(EID(0, 0), "attr2", null, "value2")
        assertEquals(1, avetCmp(f1, lesserEid))
        assertEquals(-1, avetCmp(lesserEid, f1))

        val greaterEid = FactPattern(EID(2, 0), "attr2", null, "value2")
        assertEquals(-1, avetCmp(f1, greaterEid))
        assertEquals(1, avetCmp(greaterEid, f1))

        val lesserTime = FactPattern(EID(1, 0), "attr2", 0, "value2")
        assertEquals(1, avetCmp(f1, lesserTime))
        assertEquals(-1, avetCmp(lesserTime, f1))

        val greaterTime = FactPattern(EID(1, 0), "attr2", 3, "value2")
        assertEquals(-1, avetCmp(f1, greaterTime))
        assertEquals(1, avetCmp(greaterTime, f1))

    }

}