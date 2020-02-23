package qbit.typing

import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.assertArrayEquals
import qbit.createBombWithNulls
import qbit.createBombWithoutNulls
import qbit.factorization.Destruct
import qbit.factorization.attrName
import qbit.test.model.*
import kotlin.js.JsName
import kotlin.test.*


abstract class CommonFactorizationTest(val destruct: Destruct, val attrsMap: Map<String, Attr<*>>) {

    private var gids = Gid(0, 0).nextGids()

    @BeforeTest
    fun setUp() {
        gids = Gid(0, 1).nextGids()
    }

    private val testSchema: (String) -> Attr<Any>? = { name -> attrsMap[name] }

    @JsName("Test_simple_entity_factorization")
    @Test
    fun `Test simple entity factorization`() {
        // Given the simplest entity
        val entity = TheSimplestEntity(null, "addrValue")

        // When it factorized
        val factorization = destruct(entity, testSchema, gids)

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
        val factorization = destruct(entity, testSchema, gids)

        // Then factorization contains single fact with correct gid, attribute name and value
        assertEquals(1, factorization.size, "Factorization of single entity should produce facts for one entity")
        val facts = factorization.entityFacts.values.first()
        assertEquals(1, facts.size, "Factorization of single entity with single attr should produce single fact")
        assertEquals(Gid(1), facts[0].gid)
        assertEquals(".qbit.test.model.TheSimplestEntity/scalar", facts[0].attr)
        assertEquals("addrValue", facts[0].value)
    }

    @JsName("Test_non_root_persisted_entity_factorization")
    @Test
    fun `Test non root persisted entity factorization`() {
        // Given persisted entity referenced by not persisted root
        val theEntityGid = gids.next()
        val peristedEntity = TheSimplestEntity(theEntityGid.value(), "Persisted entity")
        val ref = EntityWithRef(null, peristedEntity)

        // When it factorized
        val factorization = destruct(ref, testSchema, gids)

        // Then eavs for the entity has the same gid
        val theEntityEavs = factorization.entityFacts[peristedEntity]!!
        assertTrue(
            theEntityEavs.all { it.gid == theEntityGid },
            "Expected gid = $theEntityGid, actual gids = ${theEntityEavs.map { it.gid }}"
        )
    }

    @JsName("Test_entity_with_ref_factorization")
    @Test
    fun `Test entity with ref factorization`() {
        // Given entity graph with two entities
        val entity = EntityWithRef(null, TheSimplestEntity(null, "addrValue"))

        // When it factorized
        val factorization = destruct(entity, testSchema, gids)

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

    @JsName("Test_that_factorization_of_ref_produces_eav_with_value_type_is_Gid")
    @Test
    fun `Test that factorization of ref produces eav with value type is Gid`() {
        // Given entity graph with two entities
        val entity = EntityWithRef(null, TheSimplestEntity(null, "addrValue"))

        // When it factorized
        val factorization = destruct(entity, testSchema, gids)

        // Then factorization of root entity contains eav with value type == Gid
        val root = factorization.entityFacts[entity]!!
        assertTrue(
            root[0].value is Gid,
            "Value of type ${Gid::class} is expected, but got value of type ${root[0].value::class}"
        )
    }

    @JsName("Test_entity_with_scalars_list_factorization")
    @Test
    fun `Test entity with scalars list factorization`() {
        // Given an entity with scalars list
        val entity = EntityWithScalarList(null, listOf(0, 1, 2))

        // When it factorized
        val factorization = destruct(entity, testSchema, gids)

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
        val factorization = destruct(root, testSchema, gids)

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


    @JsName("Destruction_of_graph_with_entity_with_list_of_nullable_elements_in_props_should_fail")
    @Test
    fun `Destruction of graph with entity with list of nullable elements in props should fail`() {
        val ex = assertFailsWith<QBitException> {
            destruct(
                ListOfNullablesHolder(null, ListOfNullables(null, listOf(null), listOf(null))), testSchema, gids
            )
        }
        assertEquals(
            "List of nullable elements is not supported. Properties: qbit.test.model.ListOfNullables.(lst,refLst)",
            ex.message
        )
    }

    @JsName("Test_destruction_of_entity_with_null_list")
    @Test
    fun `Test destruction of entity with null list`() {
        val facts = destruct(NullableList(null, null, 0), testSchema, gids)
        assertEquals(1, facts.size, "Only fact for placeholder should be generated")
    }

    @JsName("Test_that_destruction_of_entity_graph_where_the_same_not_persisted_entity_occurs_multiple_times_produces_factorization_that_refers_to_the_entity_using_the_same_Gid")
    @Test
    fun `Test that destruction of entity graph where the same not persisted entity occurs multiple times produces factorization that refers to the entity using the same Gid`() {
        // Given no persisted entity and entity that refers to the entity using several references
        val theEntity = TheSimplestEntity(null, "theEntity")
        val referringEntity = EntityWithRefList(null, listOf(theEntity, theEntity))

        // When referencing entity is factorized
        val factorization = destruct(referringEntity, testSchema, gids)

        // Then factorization of referencing entity contains two facts with the same value
        val referrencingFacts = factorization.entityFacts[referringEntity]!!
        assertTrue(referrencingFacts.size == 2, "Expected")
        assertEquals(referrencingFacts[0].value, referrencingFacts[1].value)

        // And the entity has single eav
        val theEntityFacts = factorization.entityFacts[theEntity]!!
        assertTrue(theEntityFacts.size == 1, "Expected single fact but got $theEntityFacts")
        assertEquals(".qbit.test.model.TheSimplestEntity/scalar", theEntityFacts[0].attr)
        assertEquals("theEntity", theEntityFacts[0].value)
    }

    @JsName("Test_destruction_of_graph_with_different_objects_for_the_same_entity_state")
    @Test
    fun `Test destruction of graph with different objects for the same entity state`() {
        // Given entity graph, that contains two different object for same gid with same state
        val theScientistGid = gids.next()
        val theCountryGid = gids.next()
        val s1 = Scientist(
            theScientistGid.value(),
            1,
            "Name",
            emptyList(),
            Country(theCountryGid.value(), "Country", null),
            null
        )
        val s2 = Scientist(
            theScientistGid.value(),
            1,
            "Name",
            emptyList(),
            Country(theCountryGid.value(), "Country", null),
            null
        )
        val rg = ResearchGroup(null, listOf(s1, s2))

        val factorization = destruct(rg, testSchema, gids)

        // When it factorized
        val factorizationGids = factorization.map { it.gid }.toSet()

        // todo: remove manual deduplication, when it will be moved into EntityGraphFactorization
        // Then factorization contains only single set of the entity's eavs
        assertEquals(
            3,
            factorizationGids.size,
            "Expected facts for 3 entities (group, scientist and country), but got ${factorizationGids.size}"
        )

        val theScientistNameEavs = factorization.distinct()
            .filter { it.gid == theScientistGid && it.attr == ".qbit.test.model.Scientist/name" }

        assertEquals(
            1,
            theScientistNameEavs.size,
            "Expected single eav for the scientist name, but got ${theScientistNameEavs.size}"
        )
    }

    @JsName("Test_destruction_of_entity_with_null_scalar")
    @Test
    fun `Test destruction of entity with null scalar`() {
        // Given an entity with null for nullable scalar attribute
        val entityWithNullScalar = NullableScalar(null, null, 0)

        // When it factorized
        val facts = destruct(entityWithNullScalar, testSchema, gids)

        // Than its factorzation contains single eav for placeholder
        assertEquals(1, facts.size, "Only fact for placeholder should be generated")
        assertEquals(".qbit.test.model.NullableScalar/placeholder", facts.first().attr)
        assertEquals(0L, facts.first().value)
    }

    @JsName("Test_bomb_with_nulls_deconstruction")
    @Test
    fun `Test bomb with nulls deconstruction`() {
        val facts = destruct(createBombWithNulls(Gid(2, 102).value()), testSchema, gids)
        assertEquals(40, facts.size)
    }


    @JsName("Test_bomb_without_nulls_deconstruction")
    @Test
    fun `Test bomb without nulls deconstruction`() {
        val facts = destruct(createBombWithoutNulls(Gid(2, 102).value()), testSchema, gids)
        assertEquals(101, facts.size)
    }

    @JsName("Test_serialization_of_list_of_primitives")
    @Test
    fun `Test serialization of list of primitives`() {
        // Given an entity with ref to entity with non-null value of nullable primitive
        val root = EntityWithRefToNullableInt(null, NullableIntEntity(null, 2))

        // When it factorized
        val factorization = destruct(root, testSchema, gids)

        // Then it contains eav for primitive value with correct attribute
        assertTrue(
            factorization.any { it.attr == ".qbit.test.model.NullableIntEntity/int" && it.value == 2 },
            "Cannot find expected eav for NullableIntEntity.int in ${factorization.toList()}"
        )
    }

    @JsName("Test_byte_array_factorization")
    @Test
    fun `Test byte array factorization`() {
        // Given entity with byte array attr
        val byteArray = ByteArrayEntity(null, byteArrayOf(1, 2, 3))

        // When it factorized
        val factorization = destruct(byteArray, testSchema, gids)

        // Then it's factorization contains eav with byte array
        val eav = factorization.toList()[0]
        assertEquals(".qbit.test.model.ByteArrayEntity/byteArray", eav.attr)
        assertArrayEquals(byteArrayOf(1, 2, 3), eav.value as ByteArray)
    }

    @JsName("Test_list_of_byte_arrays_factorization")
    @Test
    fun `Test byte list of arrays factorization`() {
        // Given entity with list of byte arrays attr
        val firstByteArray = byteArrayOf(11, 12, 13)
        val secondByteArray = byteArrayOf(21, 22, 23)
        val thirdByteArray = byteArrayOf(31, 32, 33)
        val listOfByteArrays = ListOfByteArraysEntity(null, listOf(firstByteArray, secondByteArray, thirdByteArray))

        // When it factorized
        val factorization = destruct(listOfByteArrays, testSchema, gids)

        // Then it's factorization contains eav with byte array
        val eav = factorization.toList()[0]
        assertEquals(".qbit.test.model.ListOfByteArraysEntity/byteArrays", eav.attr)
        assertArrayEquals(firstByteArray, eav.value as ByteArray)
        assertArrayEquals(secondByteArray, factorization.toList()[1].value as ByteArray)
        assertArrayEquals(thirdByteArray, factorization.toList()[2].value as ByteArray)
    }

    @JsName("Test_SerialDescriptor_to_attr_name_conversion")
    @Test
    fun `Test SerialDescriptor to attr name conversion`() {
        assertEquals(
            ".qbit.test.model.TheSimplestEntity/scalar",
            attrName(TheSimplestEntity.serializer().descriptor, 1)
        )
    }

    // todo: entity tree

}