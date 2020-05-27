package qbit.typing

import kotlinx.serialization.Serializable
import qbit.GidEntity
import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.api.model.Eav
import qbit.api.model.impl.QTombstone
import qbit.assertArrayEquals
import qbit.createBombWithNulls
import qbit.createBombWithoutNulls
import qbit.factoring.Factor
import qbit.factoring.serializatoin.AttrName
import qbit.test.model.*
import kotlin.js.JsName
import kotlin.test.*


abstract class CommonFactoringTest(val factor: Factor, val attrsMap: Map<String, Attr<*>>) {

    private var gids = Gid(0, 0).nextGids()

    @BeforeTest
    fun setUp() {
        gids = Gid(0, 1).nextGids()
    }

    private val testSchema: (String) -> Attr<Any>? = { name -> attrsMap[name] }

    @JsName("Test_simple_entity_factoring")
    @Test
    fun `Test simple entity factoring`() {
        // Given the simplest entity
        val entity = TheSimplestEntity(null, "addrValue")

        // When it factored
        val factoring = factor(entity, testSchema, gids)

        // Then factoring contains single fact with correct attribute name and value
        assertEquals(1, factoring.size, "Factoring of single entity should produce facts for one entity")
        val facts = factoring.entityFacts.values.first()
        assertEquals(1, facts.size, "Factoring of single entity with single attr should produce single fact")
        assertEquals("TheSimplestEntity/scalar", facts[0].attr)
        assertEquals("addrValue", facts[0].value)
    }

    @JsName("Test_simple_persisted_entity_factoring")
    @Test
    fun `Test simple persisted entity factoring`() {
        // Given a persisted enitity (with id != null)
        val entity = TheSimplestEntity(1, "addrValue")

        // When it factored
        val factoring = factor(entity, testSchema, gids)

        // Then factoring contains single fact with correct gid, attribute name and value
        assertEquals(1, factoring.size, "Factoring of single entity should produce facts for one entity")
        val facts = factoring.entityFacts.values.first()
        assertEquals(1, facts.size, "Factoring of single entity with single attr should produce single fact")
        assertEquals(Gid(1), facts[0].gid)
        assertEquals("TheSimplestEntity/scalar", facts[0].attr)
        assertEquals("addrValue", facts[0].value)
    }

    @JsName("Test_non_root_persisted_entity_factoring")
    @Test
    fun `Test non root persisted entity factoring`() {
        // Given persisted entity referenced by not persisted root
        val theEntityGid = gids.next()
        val peristedEntity = TheSimplestEntity(theEntityGid.value(), "Persisted entity")
        val ref = EntityWithRef(null, peristedEntity)

        // When it factored
        val factoring = factor(ref, testSchema, gids)

        // Then eavs for the entity has the same gid
        val theEntityEavs = factoring.entityFacts[peristedEntity]!!
        assertTrue(
            theEntityEavs.all { it.gid == theEntityGid },
            "Expected gid = $theEntityGid, actual gids = ${theEntityEavs.map { it.gid }}"
        )
    }

    @JsName("Test_entity_with_ref_factoring")
    @Test
    fun `Test entity with ref factoring`() {
        // Given entity graph with two entities
        val entity = EntityWithRef(null, TheSimplestEntity(null, "addrValue"))

        // When it factored
        val factoring = factor(entity, testSchema, gids)

        // Then factoring contains fact for both entities in graph
        val root = factoring.entityFacts[entity]
        val referredEntity = factoring.entityFacts[entity.ref]

        assertEquals(2, factoring.size, "Factoring of entity with ref should produce facts for both entities")

        assertNotNull(root, "There is no factoring for root entity")
        assertNotNull(referredEntity, "There is no factoring for referred entity")

        assertEquals(2, factoring.size, "Factoring of two entities with single attr should produce two facts")

        assertEquals("EntityWithRef/ref", root[0].attr)

        assertEquals(root[0].value, referredEntity[0].gid)

        assertEquals("TheSimplestEntity/scalar", referredEntity[0].attr)
        assertEquals("addrValue", referredEntity[0].value)
    }

    @JsName("Test_that_factoring_of_ref_produces_eav_with_value_type_is_Gid")
    @Test
    fun `Test that factoring of ref produces eav with value type is Gid`() {
        // Given entity graph with two entities
        val entity = EntityWithRef(null, TheSimplestEntity(null, "addrValue"))

        // When it factored
        val factoring = factor(entity, testSchema, gids)

        // Then factoring of root entity contains eav with value type == Gid
        val root = factoring.entityFacts[entity]!!
        assertTrue(
            root[0].value is Gid,
            "Value of type ${Gid::class} is expected, but got value of type ${root[0].value::class}"
        )
    }

    @JsName("Test_entity_with_scalars_list_factoring")
    @Test
    fun `Test entity with scalars list factoring`() {
        // Given an entity with scalars list
        val entity = EntityWithScalarList(null, listOf(0, 1, 2))

        // When it factored
        val factoring = factor(entity, testSchema, gids)

        // Then it contains eav for each item in the list in the same order
        assertEquals(3, factoring.size)
        val entityEavs = factoring.entityFacts[entity]!!

        assertEquals(0, entityEavs[0].value)
        assertEquals(1, entityEavs[1].value)
        assertEquals(2, entityEavs[2].value)

        // And eavs has correct attr
        assertEquals("EntityWithScalarList/scalars", entityEavs[0].attr)
    }

    @JsName("Test_entity_with_refs_list_factoring")
    @Test
    fun `Test entity with refs list factoring`() {
        // Given an entity with refs list
        val firstReferred = TheSimplestEntity(null, "0")
        val secondReferred = TheSimplestEntity(null, "1")
        val root = EntityWithRefList(null, listOf(firstReferred, secondReferred))

        // When it factored
        val factoring = factor(root, testSchema, gids)

        // Then it contains eav for each item in the list in the same order and for each scalar value
        assertEquals(4, factoring.size)
        val rootEavs = factoring.entityFacts[root]!!
        val firstReferredEavs = factoring.entityFacts[firstReferred]!!
        val secondReferredEavs = factoring.entityFacts[secondReferred]!!

        assertTrue(rootEavs[0].value is Gid)
        assertTrue(rootEavs[1].value is Gid)
        assertNotEquals(rootEavs[0], rootEavs[1])
        assertEquals("0", firstReferredEavs[0].value)
        assertEquals("1", secondReferredEavs[0].value)

        // And eavs has correct attr
        assertEquals("EntityWithRefList/refs", rootEavs[0].attr)
        assertEquals("TheSimplestEntity/scalar", firstReferredEavs[0].attr)
    }


    @Ignore // move to schema tests
    @JsName("Destruction_of_graph_with_entity_with_list_of_nullable_elements_in_props_should_fail")
    @Test
    fun `Destruction of graph with entity with list of nullable elements in props should fail`() {
        val ex = assertFailsWith<QBitException> {
            factor(
                ListOfNullablesHolder(null, ListOfNullables(null, listOf(null), listOf(null))), testSchema, gids
            )
        }
        assertEquals(
            "List of nullable elements is not supported. Properties: ListOfNullables.(lst,refLst)",
            ex.message
        )
    }

    @JsName("Test_destruction_of_entity_with_null_list")
    @Test
    fun `Test destruction of entity with null list`() {
        val facts = factor(NullableList(null, null, 0), testSchema, gids)
        assertEquals(1, facts.size, "Only fact for placeholder should be generated")
    }

    @JsName("Test_that_destruction_of_entity_graph_where_the_same_not_persisted_entity_occurs_multiple_times_produces_factoring_that_refers_to_the_entity_using_the_same_Gid")
    @Test
    fun `Test that destruction of entity graph where the same not persisted entity occurs multiple times produces factoring that refers to the entity using the same Gid`() {
        // Given no persisted entity and entity that refers to the entity using several references
        val theEntity = TheSimplestEntity(null, "theEntity")
        val referringEntity = EntityWithRefList(null, listOf(theEntity, theEntity))

        // When referencing entity is factored
        val factoring = factor(referringEntity, testSchema, gids)

        // Then factoring of referencing entity contains two facts with the same value
        val referrencingFacts = factoring.entityFacts[referringEntity]!!
        assertTrue(referrencingFacts.size == 2, "Expected two referencing facts, but got $referrencingFacts")
        assertEquals(referrencingFacts[0].value, referrencingFacts[1].value)

        // And the entity has single eav
        val theEntityFacts = factoring.entityFacts[theEntity]!!
        assertTrue(theEntityFacts.size == 1, "Expected single fact but got $theEntityFacts")
        assertEquals("TheSimplestEntity/scalar", theEntityFacts[0].attr)
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

        val factoring = factor(rg, testSchema, gids)

        // When it factored
        val factoringGids = factoring.map { it.gid }.toSet()

        // todo: remove manual deduplication, when it will be moved into EntityGraphFactorization
        // Then factoring contains only single set of the entity's eavs
        assertEquals(
            3,
            factoringGids.size,
            "Expected facts for 3 entities (group, scientist and country), but got ${factoringGids.size}"
        )

        val theScientistNameEavs = factoring.distinct()
            .filter { it.gid == theScientistGid && it.attr == "Scientist/name" }

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

        // When it factored
        val facts = factor(entityWithNullScalar, testSchema, gids)

        // Than its factorzation contains single eav for placeholder
        assertEquals(1, facts.size, "Only fact for placeholder should be generated")
        assertEquals("NullableScalar/placeholder", facts.first().attr)
        assertEquals(0L, facts.first().value)
    }

    @JsName("Test_bomb_with_nulls_deconstruction")
    @Test
    fun `Test bomb with nulls deconstruction`() {
        val facts = factor(createBombWithNulls(Gid(2, 97).value()), testSchema, gids)
        assertEquals(40, facts.size)
    }


    @JsName("Test_bomb_without_nulls_deconstruction")
    @Test
    fun `Test bomb without nulls deconstruction`() {
        val facts = factor(createBombWithoutNulls(Gid(2, 97).value()), testSchema, gids)
        assertEquals(102, facts.size)
    }

    @JsName("Test_serialization_of_list_of_primitives")
    @Test
    fun `Test serialization of list of primitives`() {
        // Given an entity with ref to entity with non-null value of nullable primitive
        val root = EntityWithRefToNullableInt(null, NullableIntEntity(null, 2))

        // When it factored
        val factoring = factor(root, testSchema, gids)

        // Then it contains eav for primitive value with correct attribute
        assertTrue(
            factoring.any { it.attr == "NullableIntEntity/int" && it.value == 2 },
            "Cannot find expected eav for NullableIntEntity.int in ${factoring.toList()}"
        )
    }

    @JsName("Test_byte_array_factoring")
    @Test
    fun `Test byte array factoring`() {
        // Given entity with byte array attr
        val byteArray = ByteArrayEntity(null, byteArrayOf(1, 2, 3))

        // When it factored
        val factoring = factor(byteArray, testSchema, gids)

        // Then it's factoring contains eav with byte array
        val eav = factoring.toList()[0]
        assertEquals("ByteArrayEntity/byteArray", eav.attr)
        assertArrayEquals(byteArrayOf(1, 2, 3), eav.value as ByteArray)
    }

    @JsName("Test_list_of_byte_arrays_factoring")
    @Test
    fun `Test byte list of arrays factoring`() {
        // Given entity with list of byte arrays attr
        val firstByteArray = byteArrayOf(11, 12, 13)
        val secondByteArray = byteArrayOf(21, 22, 23)
        val thirdByteArray = byteArrayOf(31, 32, 33)
        val listOfByteArrays = ListOfByteArraysEntity(null, listOf(firstByteArray, secondByteArray, thirdByteArray))

        // When it factored
        val factoring = factor(listOfByteArrays, testSchema, gids)

        // Then it's factoring contains eav with byte array
        val eav = factoring.toList()[0]
        assertEquals("ListOfByteArraysEntity/byteArrays", eav.attr)
        assertArrayEquals(firstByteArray, eav.value as ByteArray)
        assertArrayEquals(secondByteArray, factoring.toList()[1].value as ByteArray)
        assertArrayEquals(thirdByteArray, factoring.toList()[2].value as ByteArray)
    }

    @JsName("Test_entity_with_nullable_Gid_id_factoring")
    @Test
    fun `Test entity with Gid? id factoring`() {
        // Given an entity identified by object of Gid? type
        val entityGid = Gid(1, 1)
        val anEntity = GidEntity(entityGid, true)

        // When it factored
        val factoring = factor(anEntity, testSchema, gids)

        // Then it's eavs gid's is equal to the gid
        assertTrue(factoring.all { it.gid == entityGid })
    }

    @JsName("Test_entity_with_Gid_id_factoring")
    @Test
    fun `Test entity with Gid id factoring`() {
        // Given an entity identified by object of Gid type
        val entityGid = Gid(1, 1)
        val anEntity = NotNullableGidEntity(entityGid, true)

        // When it factored
        val factoring = factor(anEntity, testSchema, gids)

        // Then it's eavs gid's is equal to the gid
        assertTrue(factoring.all { it.gid == entityGid })
    }

    @JsName("Test_SerialDescriptor_to_attr_name_conversion")
    @Test
    fun `Test SerialDescriptor to attr name conversion`() {
        assertEquals(
            "TheSimplestEntity/scalar",
            AttrName(TheSimplestEntity.serializer().descriptor, 1).asString()
        )
    }

    @JsName("Test_factoring_of_tombstone")
    @Test
    fun `Test factoring of tombstone`() {
        // Given id only entity
        val anEntity = QTombstone(Gid(0, 1))

        // When it factored
        val factoring = factor(anEntity, testSchema, gids)

        // Then it's factoring contains single placeholder eav
        assertEquals(1, factoring.size)
        assertEquals(Eav(Gid(0, 1), "qbit.api/tombstone", true), factoring.first())
    }

    @JsName("Test_check_for_factoring_of_different_states_for_the_same_entity")
    @Test
    fun `Test check for factoring of different states for the same entity`() {
        // Given two different states for the same entity and refs to them
        val state1 = TheSimplestEntity(Gid(2, 0).value(), "0")
        val state2 = TheSimplestEntity(Gid(2, 0).value(), "1")
        val ref = EntityWithRefList(null, listOf(state1, state2))

        // Then it fails, when it factored
        val ex = assertFailsWith(QBitException::class) {
            factor(ref, testSchema, gids)
        }
        assertTrue(ex.message?.contains("Entity ${Gid(2, 0)} has several different states to store") == true, "Actual message: ${ex.message}")
    }

    @Ignore // enable when eav's deduplication will be moved into factoring
    @JsName("Test_factoring_of_objects_with_identical_states")
    @Test
    fun `Test factoring of objects with identical states`() {
        // Given two different states for the same entity and refs to them
        val state1 = TheSimplestEntity(Gid(2, 0).value(), "1")
        val state2 = TheSimplestEntity(Gid(2, 0).value(), "1")
        val ref = EntityWithRefList(null, listOf(state1, state2))

        // When it factored
        val factoring = factor(ref, testSchema, gids)

        // Then factoring contains the same list for both states
        assertEquals(factoring.entityFacts[state1], factoring.entityFacts[state2])

        // And factoring contains only 3 eavs, 2 refs and one scalar
        assertEquals(3, factoring.size)
    }

    @JsName("Test_that_persisting_of_new_entity_with_list_attr_pulls_only_one_new_gid")
    @Test
    fun `Test that persisting of new entity with list attr pulls only one new gid`() {
        // Given an entity with list of primitives and gids source
        val anEntity = EntityWithScalarList(null, listOf(1, 2, 3))
        val baseGid = gids.next()

        // When it factored
        factor(anEntity, testSchema, gids)

        // Then only one gid is pulled from source
        assertEquals(
            gids.next().value(),
            baseGid.value() + 2,
            "Destruction of entity with list has pulled gid for a list"
        )
    }

    @JsName("Test_parent_to_children_tree_factoring")
    @Test
    fun `Test parent to children tree factoring`() {
        // Given parent to children tree
        val leaf11 = ParentToChildrenTreeEntity(null, "leaf11", emptyList())
        val leaf12 = ParentToChildrenTreeEntity(null, "leaf12", emptyList())
        val node1 = ParentToChildrenTreeEntity(null, "node1", listOf(leaf11, leaf12))
        val leaf21 = ParentToChildrenTreeEntity(null, "leaf21", emptyList())
        val leaf22 = ParentToChildrenTreeEntity(null, "leaf22", emptyList())
        val node2 = ParentToChildrenTreeEntity(null, "node2", listOf(leaf21, leaf22))
        val root = ParentToChildrenTreeEntity(null, "root", listOf(node1, node2))

        // When it factored
        val factoring = factor(root, testSchema, gids)

        // Then factoring contains eavs for all entities
        assertEquals(13, factoring.size)
    }

    @JsName("Test_factoring_of_an_entity_with_ref_to_the_same_entity_via_different_attributes")
    @Test
    fun `Test factoring of an entity with ref to the same entity via different attributes`() {
        // Given an entity referencing the same entity view different attribytes
        val referredEntity = TheSimplestEntity(null, "1")
        val referree = EntityWithRefsToSameType(null, referredEntity, referredEntity)

        // When it factored
        val factoring = factor(referree, testSchema, gids)

        // Then factoring contains 3 eavs - referree.ref1, referree.ref2, referredEntity.scalar
        assertEquals(3, factoring.size)
        assertEquals(2, factoring.entityFacts[referree]!!.size)
    }

}

@Serializable
data class NotNullableGidEntity(val id: Gid, val bool: Boolean)

