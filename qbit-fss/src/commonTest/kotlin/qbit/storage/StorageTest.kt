package qbit.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.serializersModuleOf
import qbit.api.model.Attr
import qbit.api.model.impl.QTombstone
import qbit.api.system.Instance
import qbit.factorization.KSFactorization
import qbit.ns.Namespace
import qbit.ns.ns
import qbit.ns.root
import qbit.qbit
import qbit.spi.Storage
import qbit.spi.copyStorage
import qbit.test.model.*
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

val serializersMap: Map<KClass<*>, KSerializer<*>> = mapOf<KClass<*>, KSerializer<*>>(
    Scientist::class to Scientist.serializer(),
    Attr::class to Attr.serializer(FakeSerializer<Any>()),
    Region::class to Region.serializer(),
    Country::class to Country.serializer(),
    Instance::class to Instance.serializer(),
    ResearchGroup::class to ResearchGroup.serializer(),
    IntEntity::class to IntEntity.serializer(),
    Bomb::class to Bomb.serializer(),
    QTombstone::class to QTombstone.serializer()
)
val testSchemaFactorization = KSFactorization(serializersModuleOf(serializersMap))

// It's duplication of the same class in qbit-core - study multiplatform builds and get rid of this duplication
abstract class StorageTest {

    abstract fun storage(): Storage

    @Test
    fun testStorage() {
        val storage = storage()
        val rootBytes = byteArrayOf(0, 0, 0, 0)
        val subBytes = byteArrayOf(1, 1, 1, 1)
        val rootNs = Namespace("test-root")
        val subNs = rootNs.subNs("test-sub")

        storage.add(rootNs["root-data"], rootBytes)
        storage.add(subNs["sub-data"], subBytes)

        assertArrayEquals(rootBytes, storage.load(rootNs["root-data"]))
        assertEquals(setOf(rootNs["root-data"]), storage.keys(rootNs).toSet())

        assertArrayEquals(subBytes, storage.load(subNs["sub-data"]))
        assertEquals(setOf(subNs["sub-data"]), storage.keys(subNs).toSet())
    }

    @Test
    fun testCopyNsConstructor() {
        val testNs = ns("nodes")("test")

        val origin = MemStorage()
        // initialize storage
        qbit(origin, testSchemaFactorization::ksDestruct)

        // actually it compiles
        val storage = storage()
        copyStorage(origin, storage)
        assertEquals(origin.subNamespaces(testNs.parent!!), storage.subNamespaces(testNs.parent!!))
        assertEquals(storage.subNamespaces(root).sortedBy { it.name }, listOf(ns("nodes"), ns("refs")).sortedBy { it.name })
    }

}


fun assertArrayEquals(arr1: Array<*>?, arr2: Array<*>?) {
    arr1!!; arr2!!
    assertEquals(arr1.size, arr2.size)
    (arr1 zip arr2).forEach { assertEquals(it.first, it.second) }
}

fun assertArrayEquals(arr1: ByteArray?, arr2: ByteArray?) {
    arr1!!; arr2!!
    assertEquals(arr1.size, arr2.size)
    (arr1 zip arr2).forEach { assertEquals(it.first, it.second) }
}