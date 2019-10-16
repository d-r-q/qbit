package qbit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

data class LList(val next: LList?)

private data class SubSecondaryTest(val prop1: String, val prop2: String) {

    constructor(prop1: String) : this(prop1, "prop2")

}

private data class RenamedSecondaryTest(val prop1: String, val prop2: String) {

    constructor(notAprop1: String, notAprop2: Int) : this(notAprop1, notAprop2.toString())

    var notAprop1: String = ""

    var notAprop2: Int = 1

}

private data class CustomCopy(val prop1: String, val prop2: String) {

    fun copy(prop1: Int = 1, prop2: String = this.prop2): CustomCopy {
        return CustomCopy("", "")
    }
}

class ReflectionTest {

    @Test
    fun `default should correctly handle cycles with optional refs`() {
        val default = default(LList::class)
        assertNotNull(default)
    }

    @Test
    fun `test find mutable properties`() {
        val mutProps = findMutableProperties(Scientist::class)
        assertEquals(1, mutProps.size)
        assertEquals(Scientist::reviewer, mutProps[0])
    }

    @Test
    fun `test find primary constructor of type with single constructor`() {
        assertNotNull(findPrimaryConstructor(Scientist::class))
    }

    @Test
    fun `test find primary constructor of type with secondary constructor with lesser paramas`() {
        val constr = findPrimaryConstructor(SubSecondaryTest::class)
        assertNotNull(constr)
        assertEquals(2, constr.parameters.size)
    }

    @Test
    fun `test find primary constructor of type with secondary constructor with param names matches simple properties`() {
        val constr = findPrimaryConstructor(RenamedSecondaryTest::class)
        assertNotNull(constr)
        assertEquals(2, constr.parameters.size)
        assertEquals(String::class, constr.parameters[0].type.classifier)
        assertEquals(String::class, constr.parameters[1].type.classifier)
    }

    @Test
    fun `test find primary constructor of type with copy with the same param names`() {
        val constr = findPrimaryConstructor(CustomCopy::class)
        assertNotNull(constr)
        assertEquals(2, constr.parameters.size)
        assertEquals(String::class, constr.parameters[0].type.classifier)
        assertEquals(String::class, constr.parameters[1].type.classifier)
    }

    @Test
    fun `Test find setable props`() {
        val setableProps = setableProps(Scientist::class)
        assertEquals(1, setableProps.size)
        assertEquals("reviewer", setableProps[0].name)
    }

    @Test
    fun `Test find property for attr`() {
        assertEquals("country", Scientist::class.propertyFor(Scientists.country)?.name)
    }

}