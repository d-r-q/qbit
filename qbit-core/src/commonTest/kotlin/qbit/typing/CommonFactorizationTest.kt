package qbit.typing

import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.api.model.QInt
import qbit.api.model.QRef
import qbit.api.model.QString
import qbit.factorization.Destruct
import qbit.factorization.attrName
import qbit.test.model.TheSimplestEntity
import qbit.test.model.EntityWithRef
import qbit.test.model.EntityWithRefList
import qbit.test.model.EntityWithScalarList
import kotlin.js.JsName
import kotlin.test.*


abstract class CommonFactorizationTest(val destruct: Destruct) {

    private var gids = Gid(0, 0).nextGids()

    @BeforeTest
    fun setUp() {
        gids = Gid(0, 1).nextGids()
    }

    private val testSchema = mapOf(
        ".qbit.test.model.TheSimplestEntity/scalar" to Attr<String>(
            gids.next(),
            ".qbit.test.model.TheSimplestEntity/scalar",
            QString.code,
            unique = false,
            list = false
        ),

        ".qbit.test.model.EntityWithRef/ref" to Attr<TheSimplestEntity>(
            gids.next(),
            ".qbit.test.model.EntityWithRef/ref",
            QRef.code,
            unique = false,
            list = false
        ),

        ".qbit.test.model.EntityWithScalarList/scalars" to Attr<List<Int>>(
            gids.next(),
            ".qbit.test.model.EntityWithScalarList/scalars",
            QInt.code,
            unique = false,
            list = true
        ),

        ".qbit.test.model.EntityWithRefList/refs" to Attr<List<TheSimplestEntity>>(
            gids.next(),
            ".qbit.test.model.EntityWithRefList/refs",
            QRef.code,
            unique = false,
            list = true
        )
    )

    @JsName("Test_simple_entity_factorization")
    @Test
    fun `Test simple entity factorization`() {
        // Given the simplest entity
        val entity = TheSimplestEntity(null, "addrValue")

        // When it factorized
        val factorization = destruct(entity, testSchema::get, gids)

        // Then factorization contains single fact with correct attribute name and value
        assertEquals(1, factorization.size, "Factorization of single entity should produce facts for one entity")
        val facts = factorization.entityFacts.values.first()
        assertEquals(1, facts.size, "Factorization of single entity with single attr should produce single fact")
        assertEquals(".qbit.test.model.TheSimplestEntity/scalar", facts[0].attr)
        assertEquals("addrValue", facts[0].value)
    }

    @JsName("Test_simple_persisted_entity_factorization")
    @Test
    fun `Test simple persisted entity factorization`() {
        // Given a persisted enitity (with id != null)
        val entity = TheSimplestEntity(1, "addrValue")

        // When it factorized
        val factorization = destruct(entity, testSchema::get, gids)

        // Then factorization contains single fact with correct gid, attribute name and value
        assertEquals(1, factorization.size, "Factorization of single entity should produce facts for one entity")
        val facts = factorization.entityFacts.values.first()
        assertEquals(1, facts.size, "Factorization of single entity with single attr should produce single fact")
        assertEquals(Gid(1), facts[0].gid)
        assertEquals(".qbit.test.model.TheSimplestEntity/scalar", facts[0].attr)
        assertEquals("addrValue", facts[0].value)
    }

    @JsName("Test_entity_with_ref_factorization")
    @Test
    fun `Test entity with ref factorization`() {
        // Given entity graph with two entities
        val entity = EntityWithRef(null, TheSimplestEntity(null, "addrValue"))

        // When it factorized
        val factorization = destruct(entity, testSchema::get, gids)

        // Then factorization contains fact for both entities in graph
        val root = factorization.entityFacts[entity]
        val referredEntity = factorization.entityFacts[entity.ref]

        assertEquals(2, factorization.size, "Factorization of entity with ref should produce facts for both entities")

        assertNotNull(root, "There is no factorization for root entity")
        assertNotNull(referredEntity, "There is no factorization for referred entity")

        assertEquals(2, factorization.size, "Factorization of two entities with single attr should produce two facts")

        assertEquals(".qbit.test.model.EntityWithRef/ref", root[0].attr)

        assertEquals(root[0].value, referredEntity[0].gid)

        assertEquals(".qbit.test.model.TheSimplestEntity/scalar", referredEntity[0].attr)
        assertEquals("addrValue", referredEntity[0].value)
    }

    @JsName("Test_that_factorization_of_ref_produces_eav_with_value_type_==_Gid")
    @Test
    fun `Test that factorization of ref produces eav with value type == Gid`() {
        // Given entity graph with two entities
        val entity = EntityWithRef(null, TheSimplestEntity(null, "addrValue"))

        // When it factorized
        val factorization = destruct(entity, testSchema::get, gids)

        // Then factorization of root entity contains eav with value type == Gid
        val root = factorization.entityFacts[entity]!!
        assertTrue(root[0].value is Gid, "Value of type ${Gid::class} is expected, but got value of type ${root[0].value::class}")
    }

    @JsName("Test_entity_with_scalars_list_factorization")
    @Test
    fun `Test entity with scalars list factorization`() {
        // Given an entity with scalars list
        val entity = EntityWithScalarList(null, listOf(0, 1, 2))

        // When it factorized
        val factorization = destruct(entity, testSchema::get, gids)

        // Then it contains eav for each item in the list in the same order
        assertEquals(3, factorization.size)
        val entityEavs = factorization.entityFacts[entity]!!

        assertEquals(0, entityEavs[0].value)
        assertEquals(1, entityEavs[1].value)
        assertEquals(2, entityEavs[2].value)

        // And eavs has correct attr
        assertEquals(".qbit.test.model.EntityWithScalarList/scalars", entityEavs[0].attr)
    }

    @JsName("Test_entity_with_refs_list_factorization")
    @Test
    fun `Test entity with refs list factorization`() {
        // Given an entity with refs list
        val firstReferred = TheSimplestEntity(null, "0")
        val secondReferred = TheSimplestEntity(null, "1")
        val root = EntityWithRefList(null, listOf(firstReferred, secondReferred))

        // When it factorized
        val factorization = destruct(root, testSchema::get, gids)

        // Then it contains eav for each item in the list in the same order and for each scalar value
        assertEquals(4, factorization.size)
        val rootEavs = factorization.entityFacts[root]!!
        val firstReferredEavs = factorization.entityFacts[firstReferred]!!
        val secondReferredEavs = factorization.entityFacts[secondReferred]!!

        assertEquals(Gid(0, 1), rootEavs[0].value)
        assertEquals(Gid(0, 2), rootEavs[1].value)
        assertEquals("0", firstReferredEavs[0].value)
        assertEquals("1", secondReferredEavs[0].value)

        // And eavs has correct attr
        assertEquals(".qbit.test.model.EntityWithRefList/refs", rootEavs[0].attr)
        assertEquals(".qbit.test.model.TheSimplestEntity/scalar", firstReferredEavs[0].attr)
    }

    @JsName("Test_SerialDescriptor_to_attr_name_conversion")
    @Test
    fun `Test SerialDescriptor to attr name conversion`() {
        assertEquals(".qbit.test.model.TheSimplestEntity/scalar", attrName(TheSimplestEntity.serializer().descriptor, 1))
    }

}