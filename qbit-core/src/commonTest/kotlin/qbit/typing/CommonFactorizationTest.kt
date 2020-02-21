package qbit.typing

import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.*
import qbit.factorization.Destruct
import qbit.factorization.attrName
import qbit.test.model.*
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
        ),

        ".qbit.test.model.ListOfNullablesHolder/nullables" to Attr<List<ListOfNullables>>(
            gids.next(),
            ".qbit.test.model.ListOfNullablesHolder/nullables",
            QRef.code,
            unique = false,
            list = true
        ),

        ".qbit.test.model.NullableList/placeholder" to Attr<Long>(
            gids.next(),
            ".qbit.test.model.NullableList/placeholder",
            QLong.code,
            unique = false,
            list = false
        ),

        // MUser

        ".qbit.test.model.MUser/login" to Attr<String>(
            gids.next(),
            ".qbit.test.model.MUser/login",
            QString.code,
            unique = true,
            list = false
        ),

        ".qbit.test.model.MUser/strs" to Attr<List<String>>(
            gids.next(),
            ".qbit.test.model.MUser/strs",
            QString.code,
            unique = false,
            list = true
        ),

        ".qbit.test.model.MUser/theSimplestEntity" to Attr<TheSimplestEntity>(
            gids.next(),
            ".qbit.test.model.MUser/theSimplestEntity",
            QRef.code,
            unique = false,
            list = false
        ),

        ".qbit.test.model.MUser/optTheSimplestEntity" to Attr<TheSimplestEntity>(
            gids.next(),
            ".qbit.test.model.MUser/optTheSimplestEntity",
            QRef.code,
            unique = false,
            list = false
        ),

        ".qbit.test.model.MUser/theSimplestEntities" to Attr<List<TheSimplestEntity>>(
            gids.next(),
            ".qbit.test.model.MUser/theSimplestEntities",
            QRef.code,
            unique = false,
            list = true
        ),

        // Research group

        ".qbit.test.model.ResearchGroup/members" to Attr<List<Scientist>>(
            gids.next(),
            ".qbit.test.model.ResearchGroup/members",
            QRef.code,
            unique = false,
            list = true
        ),

        // Scientist

        ".qbit.test.model.Scientist/externalId" to Attr<Int>(
            gids.next(),
            ".qbit.test.model.Scientist/externalId",
            QRef.code,
            unique = true,
            list = false
        ),

        ".qbit.test.model.Scientist/name" to Attr<String>(
            gids.next(),
            ".qbit.test.model.Scientist/name",
            QRef.code,
            unique = false,
            list = false
        ),

        ".qbit.test.model.Scientist/name" to Attr<String>(
            gids.next(),
            ".qbit.test.model.Scientist/name",
            QRef.code,
            unique = false,
            list = false
        ),

        ".qbit.test.model.Scientist/nicks" to Attr<List<String>>(
            gids.next(),
            ".qbit.test.model.Scientist/nicks",
            QRef.code,
            unique = false,
            list = true
        ),

        ".qbit.test.model.Scientist/country" to Attr<Country>(
            gids.next(),
            ".qbit.test.model.Scientist/country",
            QRef.code,
            unique = false,
            list = false
        ),

        ".qbit.test.model.Scientist/reviewer" to Attr<Scientist>(
            gids.next(),
            ".qbit.test.model.Scientist/reviewer",
            QRef.code,
            unique = false,
            list = false
        ),

        // Country

        ".qbit.test.model.Country/name" to Attr<String>(
            gids.next(),
            ".qbit.test.model.Country/name",
            QRef.code,
            unique = false,
            list = false
        ),

        ".qbit.test.model.Country/population" to Attr<Int>(
            gids.next(),
            ".qbit.test.model.Country/population",
            QRef.code,
            unique = false,
            list = false
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

    @JsName("Test_non_root_persisted_entity_factorization")
    @Test
    fun `Test non root persisted entity factorization`() {
        // Given persisted entity referenced by not persisted root
        val theEntityGid = gids.next()
        val peristedEntity = TheSimplestEntity(theEntityGid.value(), "Persisted entity")
        val ref = EntityWithRef(null, peristedEntity)

        // When it factorized
        val factorization = destruct(ref, testSchema::get, gids)

        // Then eavs for the entity has the same gid
        val theEntityEavs = factorization.entityFacts[peristedEntity]!!
        assertTrue(
            theEntityEavs.all { it.gid == theEntityGid },
            "Expected gid = $theEntityGid, actual gids = ${theEntityEavs.map { it.gid }}")
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

    @JsName("Test_that_factorization_of_ref_produces_eav_with_value_type_is_Gid")
    @Test
    fun `Test that factorization of ref produces eav with value type is Gid`() {
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


    @JsName("Destruction_of_graph_with_entity_with_list_of_nullable_elements_in_props_should_fail")
    @Test
    fun `Destruction of graph with entity with list of nullable elements in props should fail`() {
        val ex = assertFailsWith<QBitException> {
            destruct(
                ListOfNullablesHolder(null, ListOfNullables(null, listOf(null), listOf(null))), testSchema::get, gids
            )
        }
        assertEquals("List of nullable elements is not supported. Properties: qbit.test.model.ListOfNullables.(lst,refLst)", ex.message)
    }

    @JsName("Test_destruction_of_entity_with_null_list")
    @Test
    fun `Test destruction of entity with null list`() {
        val facts = destruct(NullableList(null, null, 0), testSchema::get, gids)
        assertEquals(1, facts.size, "Only fact for placeholder should be generated")
    }

    @JsName("Test_that_destruction_of_entity_graph_where_the_same_not_persisted_entity_occurs_multiple_times_produces_factorization_that_refers_to_the_entity_using_the_same_Gid")
    @Test
    fun `Test that destruction of entity graph where the same not persisted entity occurs multiple times produces factorization that refers to the entity using the same Gid`() {
        // Given no persisted entity and entity that refers to the entity using several references
        val theEntity = TheSimplestEntity(null, "theEntity")
        val referringEntity = EntityWithRefList(null, listOf(theEntity, theEntity))

        // When referencing entity is factorized
        val factorization = destruct(referringEntity, testSchema::get, gids)

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

        val factorization = destruct(rg, testSchema::get, gids)

        // When it factorized
        val factorizationGids = factorization.map { it.gid }.toSet()

        // todo: remove manual deduplication, when it will be moved into EntityGraphFactorization
        // Then factorization contains only single set of the entity's eavs
        assertEquals(
            3,
            factorizationGids.size,
            "Expected facts for 3 entities (group, scientist and country), but got ${factorizationGids.size}"
        )

        val theScientistNameEavs = factorization.distinct().filter { it.gid == theScientistGid && it.attr == ".qbit.test.model.Scientist/name" }

        assertEquals(
            1,
            theScientistNameEavs.size,
            "Expected single eav for the scientist name, but got ${theScientistNameEavs.size}"
        )
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