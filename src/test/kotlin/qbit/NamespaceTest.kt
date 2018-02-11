package qbit

import org.junit.Assert.assertEquals
import org.junit.Test

class NamespaceTest {

    @Test
    fun testNamespaceOfList() {
        val n1 = Namespace("n1")
        assertEquals(n1, Namespace.of("n1"))
        val n2 = n1.subNs("n2")
        assertEquals(n2, Namespace.of("n1", "n2"))
        val n3 = n2.subNs("n3")
        assertEquals(n3, Namespace.of("n1", "n2", "n3"))
    }

}