package qbit.storage

import qbit.ns.Key
import qbit.ns.Namespace
import kotlin.test.Test
import kotlin.test.assertTrue

class YandexDiskStorageHelperMethodsTest {

    @Test
    fun generatePutResourcePath() {
        val ns = Namespace("ns")
        val sub1 = Namespace(ns, "sub1")
        val sub2 = Namespace(sub1, "sub2")
        val key = Key(sub2, "key")

        val partsArrayList = arrayListOf<List<String>>()
        var currentNamespace = key.ns
        while (currentNamespace.parent != null) {
            partsArrayList.add(currentNamespace.parts)
            currentNamespace = currentNamespace.parent!!
        }
        val result = partsArrayList.map { part -> part.joinToString("/") + "/" }.reversed()
        assertTrue(result[0] == "/ns/")
        assertTrue(result[1] == "/ns/sub1/")
        assertTrue(result[2] == "/ns/sub1/sub2/")
    }
}