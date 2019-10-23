package qbit

import qbit.ns.Namespace
import qbit.ns.root
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun testRootNsToStr() {
        assertEquals("", root.toString())
    }

    @Test
    fun testRootNsKeyToStr() {
        assertEquals("/test", root["test"].toString())
    }

    @Test
    fun testFirstLevelNsToStr() {
        assertEquals(".test1", root("test1").toString())
    }

    @Test
    fun testFirstLevelNsKeyToStr() {
        assertEquals(".test1/key", root("test1")["key"].toString())
    }

    @Test
    fun testSecondLevelNsToStr() {
        assertEquals(".test1.test2", root("test1")("test2").toString())
    }

    @Test
    fun testSecondLevelNsKeyToStr() {
        assertEquals(".test1.test2/key", root("test1")("test2")["key"].toString())
    }
}