package qbit.typing

import qbit.*
import qbit.api.gid.Gid
import qbit.api.gid.nextGids
import qbit.api.model.Attr
import qbit.api.model.StoredEntity
import qbit.api.model.impl.AttachedEntity
import qbit.test.model.*
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class SerializationTypingTest {

    private val nullGidResolver = { _: Gid -> null }

    val gids = Gid(0, 0).nextGids()

    @JsName("Test_typing_the_simplest_entity")
    @Test
    fun `Test typing the simplest entity`() {
        val map = HashMap<Gid, StoredEntity>()
        val theSimplestEntity =
            AttachedEntity(gids.next(), listOf(TheSimplestEntities.scalar to "Aleksey Lyapunov"), map::get)
        val typedEntity = typify(schemaMap::get, theSimplestEntity, TheSimplestEntity::class, testsSerialModule)
        assertEquals(typedEntity.scalar, "Aleksey Lyapunov")
    }

    @JsName("Test_simple_instantiation")
    @Test
    fun `Test simple instantiation`() {
        val ru = AttachedEntity(gids.next(),
            listOf(Countries.name to "Russia", Countries.population to 146_000_000),
            nullGidResolver
        )
        val typedRu = typify(schemaMap::get, ru, Country::class, testsSerialModule)
        assertEquals("Russia", typedRu.name)
        assertEquals(146_000_000, typedRu.population)
    }


    @JsName("Test_simple_graph_instantiation")
    @Test
    fun `Test simple graph instantiation`() {
        val map = HashMap<Gid, StoredEntity>()
        val ru = AttachedEntity(gids.next(),
            listOf(Countries.name to "Russia", Countries.population to 146_000_000),
            map::get)
        val nsk = AttachedEntity(gids.next(),
            listOf(Regions.name to "Novosibirskaya obl.", Regions.country to ru.gid),
            map::get)
        map[ru.gid] = ru
        val typedNsk = typify(schemaMap::get, nsk, Region::class, testsSerialModule)
        assertEquals("Novosibirskaya obl.", typedNsk.name)
        assertEquals("Russia", typedNsk.country.name)
    }

    @JsName("Test_instantiation_of_nullable_value_property_without_setter")
    @Test
    fun `Test instantiation of nullable value property without setter`() {

        val ru = AttachedEntity(gids.next(),
            listOf(Countries.name to "Russia", Countries.population to 146_000_000),
            nullGidResolver)
        val typedRu = typify(schemaMap::get, ru, Country::class, testsSerialModule)
        assertEquals(146_000_000, typedRu.population)
    }

    @JsName("Test_instantiation_of_nullable_ref_property_without_setter")
    @Test
    fun `Test instantiation of nullable ref property without setter`() {

        val er = AttachedEntity(gids.next(),
            listOf(Papers.name to "ER-Model"),
            nullGidResolver)
        val typedEr = typify(schemaMap::get, er, Paper::class, testsSerialModule)
        assertEquals("ER-Model", typedEr.name)
        assertNull(typedEr.editor)
    }

    @JsName("Test_instantiation_of_nullable_ref_property_with_setter")
    @Test
    fun `Test instantiation of nullable ref property with setter`() {
        val map = HashMap<Gid, StoredEntity>()
        val ru = AttachedEntity(gids.next(),
            listOf(Countries.name to "Russia", Countries.population to 146_000_000),
            map::get)
        val aLaypunov = AttachedEntity(gids.next(), listOf(Scientists.name to "Aleksey Lyapunov", Scientists.extId to 1, Scientists.nicks to emptyList<String>(), Scientists.country to ru), map::get)
        val aErshov = AttachedEntity(gids.next(), listOf(Scientists.name to "Andrey Ershov", Scientists.reviewer to aLaypunov.gid, Scientists.extId to 2, Scientists.nicks to emptyList<String>(), Scientists.country to ru), map::get)
        map[aLaypunov.gid] = aLaypunov
        map[aErshov.gid] = aErshov
        map[ru.gid] = ru
        val typedErshov = typify(schemaMap::get, aErshov, Scientist::class, testsSerialModule)
        @Suppress("UNCHECKED_CAST")
        assertEquals(aLaypunov[Scientists.name as Attr<String>], typedErshov.reviewer?.name)
    }


    @JsName("Test_instantiation_of_entity_without_fact_for_optional_property")
    @Test
    fun `Test instantiation of entity without fact for optional property`() {
        val map = HashMap<Gid, StoredEntity>()

        val e = AttachedEntity(gids.next(), listOf(NullableScalars.placeholder to 0), map::get)

        map[e.gid] = e
        val ns = typify(schemaMap::get, e, NullableScalar::class, testsSerialModule)
        // Workaround for strange failure in case of longs comparison on js platform:
        // qbit.typing
        //       SerializationTypingTest
        //         Test instantiation of entity without fact for optional property:
        //     AssertionError: Expected <0>, actual <0>.
        assertEquals("0", ns.placeholder.toString())
    }


    @JsName("Test_instantiation_of_entity_with_attribute_for_nullable_mutable_value_property")
    @Test
    fun `Test instantiation of entity with attribute for nullable mutable value property`() {
        val map = HashMap<Gid, StoredEntity>()

        val e = AttachedEntity(gids.next(), listOf(NullableScalars.scalar to 1.toByte(), NullableScalars.placeholder to 1L), map::get)

        map[e.gid] = e
        val ns = typify(schemaMap::get, e, NullableScalar::class, testsSerialModule)
        assertEquals(1L, ns.placeholder)
        assertEquals(1.toByte(), ns.scalar)
    }


    @JsName("Test_instantiation_of_entity_with_not_null_value_for_nullable_values_list_attribute")
    @Test
    fun `Test instantiation of entity with not-null value for nullable values list attribute`() {
        val map = HashMap<Gid, StoredEntity>()

        val e = AttachedEntity(gids.next(), listOf(NullableLists.lst to listOf(1.toByte()), NullableLists.placeholder to 1L), map::get)

        map[e.gid] = e
        val ns = typify(schemaMap::get, e, NullableList::class, testsSerialModule)
        assertEquals(listOf(1.toByte()), ns.lst)
    }

    @JsName("Test_instantiation_of_entity_with_not_null_value_for_nullable_ref_attribute")
    @Test
    fun `Test instantiation of entity with not-null value for nullable ref attribute`() {
        val map = HashMap<Gid, StoredEntity>()

        val refGid = gids.next()
        val r = AttachedEntity(refGid, listOf(IntEntities.int to 1), map::get)
        val e = AttachedEntity(gids.next(), listOf(NullableRefs.ref to refGid, NullableRefs.placeholder to 1L), map::get)

        map[r.gid] = r
        map[e.gid] = e
        val ns = typify(schemaMap::get, e, NullableRef::class, testsSerialModule)
        assertEquals(1, ns.ref?.int)
    }


    @JsName("Test_scalar_ref_traversing")
    @Test
    fun `Test scalar ref traversing`() {
        val map = HashMap<Gid, StoredEntity>()
        val ru = AttachedEntity(gids.next(), listOf(Countries.name to "Russia"), map::get)
        val aLaypunov = AttachedEntity(gids.next(), listOf(Scientists.name to "Aleksey Lyapunov", Scientists.extId to 1, Scientists.country to ru.gid), map::get)
        val aErshov = AttachedEntity(gids.next(), listOf(Scientists.name to "Andrey Ershov", Scientists.reviewer to aLaypunov.gid, Scientists.extId to 0, Scientists.country to ru.gid), map::get)
        map[aLaypunov.gid] = aLaypunov
        map[aErshov.gid] = aErshov
        map[ru.gid] = ru
        val typedErshov = typify(schemaMap::get, aErshov, Scientist::class, testsSerialModule)
        assertEquals("Andrey Ershov", typedErshov.name)
        assertEquals("Aleksey Lyapunov", typedErshov.reviewer?.name)
    }


    @JsName("Test_list_ref_traversing")
    @Test
    fun `Test list ref traversing`() {
        val map = HashMap<Gid, StoredEntity>()
        val ru = AttachedEntity(gids.next(), listOf(Countries.name to "Russia"), map::get)
        val aLaypunov = AttachedEntity(gids.next(), listOf(Scientists.name to "Aleksey Lyapunov", Scientists.extId to 1, Scientists.country to ru.gid), map::get)
        val aErshov = AttachedEntity(gids.next(), listOf(Scientists.name to "Andrey Ershov", Scientists.reviewer to aLaypunov.gid, Scientists.extId to 0, Scientists.country to ru.gid), map::get)
        val researchGroup = AttachedEntity(gids.next(), listOf(ResearchGroups.members to listOf(aLaypunov.gid, aErshov.gid)), map::get)
        map[aLaypunov.gid] = aLaypunov
        map[aErshov.gid] = aErshov
        map[researchGroup.gid] = researchGroup
        map[ru.gid] = ru

        val typedGroup = typify(schemaMap::get, researchGroup, ResearchGroup::class, testsSerialModule)
        assertEquals(2, typedGroup.members.size)
        assertEquals("Aleksey Lyapunov", typedGroup.members[0].name)
        assertEquals("Andrey Ershov", typedGroup.members[1].name)
    }


    @JsName("When_typing_with_GraphQuery_type_T1_that_has_mandatory_ref_to_type_T2_mandatory_props_of_T2_should_be_fetched_too")
    @Test
    fun `When typing with GraphQuery type T1, that has mandatory ref to type T2, mandatory props of T2 should be fetched too`() {
        val map = HashMap<Gid, StoredEntity>()
        val ru = AttachedEntity(gids.next(), listOf(Countries.name to "Russia"), map::get)
        val nsk = AttachedEntity(gids.next(), listOf(Regions.name to "Novosibirskaya obl.", Regions.country to ru), map::get)
        val nskCity = AttachedEntity(gids.next(), listOf(Cities.name to "Novosibirsk", Cities.region to nsk), map::get)
        map[ru.gid] = ru
        map[nsk.gid] = nsk
        map[nskCity.gid] = nskCity

        val typedNsk = typify(schemaMap::get, nskCity, City::class, testsSerialModule)
        assertEquals("Russia", typedNsk.region.country.name)
    }

    @JsName("Test_entity_with_bytearray_field_typing")
    @Test
    fun `Test entity with bytearray field typing`() {
        // Given an entity with bytearray field
        val entity = AttachedEntity(gids.next(), listOf(EntityWithByteArrays.byteArray to byteArrayOf(1, 2, 3)), nullGidResolver)

        // When it typed
        val typed = typify(schemaMap::get, entity, EntityWithByteArray::class, testsSerialModule)

        // Then it contains
        assertArrayEquals(byteArrayOf(1, 2, 3), typed.byteArray)
    }

    @JsName("Test_entity_with_empty_bytearray_field_typing")
    @Test
    fun `Test entity with empty bytearray field typing`() {
        // Given an entity with bytearray field
        val entity = AttachedEntity(gids.next(), listOf(EntityWithByteArrays.byteArray to byteArrayOf()), nullGidResolver)

        // When it typed
        val typed = typify(schemaMap::get, entity, EntityWithByteArray::class, testsSerialModule)

        // Then it contains
        assertArrayEquals(byteArrayOf(), typed.byteArray)
    }

    @JsName("Test_entity_with_null_bytearray_field_typing")
    @Test
    fun `Test entity with null bytearray field typing`() {
        // Given an entity with bytearray field
        val entity = AttachedEntity(gids.next(), listOf(), nullGidResolver)

        // When it typed
        val typed = typify(schemaMap::get, entity, EntityWithByteArray::class, testsSerialModule)

        // Then it contains
        assertNull(typed.byteArray)
    }

    @JsName("Test_entity_with_list_of_bytes_field_typing")
    @Test
    fun `Test entity with list of bytes field typing`() {
        // Given an entity with list of bytes field
        val entity = AttachedEntity(gids.next(), listOf(EntityWithListOfBytess.bytes to listOf<Byte>(1, 2, 3)), nullGidResolver)

        // When it typed
        val typed = typify(schemaMap::get, entity, EntityWithListOfBytes::class, testsSerialModule)

        // Then it contains
        assertEquals(listOf<Byte>(1, 2, 3), typed.bytes)
    }

    @JsName("Test_entity_with_list_of_bytearrays_field_typing")
    @Test
    fun `Test entity with list of bytearrays field typing`() {
        // Given an entity with list of bytearrays field
        val entity = AttachedEntity(gids.next(), listOf(EntityWithListOfByteArrays.byteArrays to listOf(byteArrayOf(1), byteArrayOf(1, 2))), nullGidResolver)

        // When it typed
        val typed = typify(schemaMap::get, entity, EntityWithListOfByteArray::class, testsSerialModule)

        // Then it contains
        assertArrayEquals(byteArrayOf(1), typed.byteArrays[0])
        assertArrayEquals(byteArrayOf(1, 2), typed.byteArrays[1])
    }

    @JsName("Test_entity_with_list_of_strings_field_typing")
    @Test
    fun `Test entity with list of strings field typing`() {
        // Given an entity with list of bytearrays field
        val entity = AttachedEntity(gids.next(), listOf(EntityWithListOfStringss.strings to listOf("1")), nullGidResolver)

        // When it typed
        val typed = typify(schemaMap::get, entity, EntityWithListOfString::class, testsSerialModule)

        // Then it contains
        assertEquals(listOf("1"), typed.strings)
    }

    @JsName("Test_entity_with_empty_list_of_strings_field_typing")
    @Test
    fun `Test entity with empty list of strings field typing`() {
        // Given an entity with list of bytearrays field
        val entity = AttachedEntity(gids.next(), listOf(EntityWithListOfStringss.strings to listOf<String>()), nullGidResolver)

        // When it typed
        val typed = typify(schemaMap::get, entity, EntityWithListOfString::class, testsSerialModule)

        // Then it contains
        assertEquals(listOf(), typed.strings)
    }

    @JsName("Test_entity_with_null_list_of_strings_field_typing")
    @Test
    fun `Test entity with null list of strings field typing`() {
        // Given an entity with list of bytearrays field
        val entity = AttachedEntity(gids.next(), listOf(), nullGidResolver)

        // When it typed
        val typed = typify(schemaMap::get, entity, EntityWithListOfString::class, testsSerialModule)

        // Then it contains
        assertNull(typed.strings)
    }

    @JsName("Test_entity_with_id_Gid_typing")
    @Test
    fun `Test entity with id Gid typing`() {
        // Given an entity with id: Gid
        val gid = gids.next()
        val entity = AttachedEntity(gid, listOf(GidEntities.bool to true), nullGidResolver)

        // When it typed
        val typed = typify(schemaMap::get, entity, GidEntity::class, internalTestsSerialModule)

        // Then it has correct gid
        assertEquals(gid, typed.id)
    }

    @JsName("Test_parent_to_children_tree_typing")
    @Test
    fun `Test parent to children tree typing`() {
        // Given three-level tree of entities
        val entities: MutableMap<Gid, StoredEntity> = HashMap()
        val leaf11 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "leaf11", ParentToChildrenTreeEntities.children to emptyList<StoredEntity>()), entities::get)
        val leaf12 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "leaf12", ParentToChildrenTreeEntities.children to emptyList<StoredEntity>()), entities::get)
        val leaf13 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "leaf13", ParentToChildrenTreeEntities.children to emptyList<StoredEntity>()), entities::get)
        val node1 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "node1", ParentToChildrenTreeEntities.children to listOf(leaf11, leaf12, leaf13)), entities::get)
        entities.putAll(listOf(leaf11, leaf12, leaf13, node1).associateBy { it.gid })

        val leaf21 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "leaf21", ParentToChildrenTreeEntities.children to emptyList<StoredEntity>()), entities::get)
        val leaf22 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "leaf22", ParentToChildrenTreeEntities.children to emptyList<StoredEntity>()), entities::get)
        val leaf23 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "leaf23", ParentToChildrenTreeEntities.children to emptyList<StoredEntity>()), entities::get)
        val node2 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "node2", ParentToChildrenTreeEntities.children to listOf(leaf21, leaf22, leaf23)), entities::get)
        entities.putAll(listOf(leaf21, leaf22, leaf23, node2).associateBy { it.gid })

        val leaf31 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "leaf31", ParentToChildrenTreeEntities.children to emptyList<StoredEntity>()), entities::get)
        val leaf32 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "leaf32", ParentToChildrenTreeEntities.children to emptyList<StoredEntity>()), entities::get)
        val leaf33 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "leaf33", ParentToChildrenTreeEntities.children to emptyList<StoredEntity>()), entities::get)
        val node3 = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "node3", ParentToChildrenTreeEntities.children to listOf(leaf31, leaf32, leaf33)), entities::get)
        entities.putAll(listOf(leaf31, leaf32, leaf33, node3).associateBy { it.gid })

        val root = AttachedEntity(gids.next(), listOf(ParentToChildrenTreeEntities.name to "root", ParentToChildrenTreeEntities.children to listOf(node1, node2, node3)), entities::get)

        // When it typed
        val typed = typify(schemaMap::get, root, ParentToChildrenTreeEntity::class, internalTestsSerialModule)

        // Then root has correct name and children count
        assertEquals("root", typed.name)
        assertEquals(3, typed.children.size)

        // And second node has correct name and children count
        assertEquals("node2", typed.children[1].name)
        assertEquals(3, typed.children[1].children.size)

        // And leaf33 has correct name and children count
        assertEquals("leaf33", typed.children[2].children[2].name)
        assertEquals(0, typed.children[2].children[2].children.size)
    }

}

