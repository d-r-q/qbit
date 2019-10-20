package qbit.typing

import qbit.*
import qbit.model.AttachedEntity
import qbit.model.Attr
import qbit.model.Gid
import qbit.model.StoredEntity
import kotlin.test.*
import qbit.Scientist as Scientist


class TypingTest {

    @Test
    fun `Test scalar ref traversing`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()
        val aLaypunov = AttachedEntity(gids.next(), mapOf<Attr<Any>, Any>(Scientists.name to "Aleksey Lyapunov"), map::get)
        val aErshov = AttachedEntity(gids.next(), mapOf(Scientists.name to "Andrey Ershov", Scientists.reviewer to aLaypunov.gid), map::get)
        map[aLaypunov.gid] = aLaypunov
        map[aErshov.gid] = aErshov
        val typing = Typing(aErshov, EagerQuery(), Scientist::class)
        assertEquals(2, typing.entities.size)
    }

    @Test
    fun `Test scalar ref cycle traversing`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()
        val sLebedevGid = gids.next()

        val aLaypunov = AttachedEntity(gids.next(), mapOf(Scientists.name to "Aleksey Lyapunov", Scientists.reviewer to sLebedevGid), map::get)
        val aErshov = AttachedEntity(gids.next(), mapOf(Scientists.name to "Andrey Ershov", Scientists.reviewer to aLaypunov.gid), map::get)
        val sLebedev = AttachedEntity(sLebedevGid, mapOf(Scientists.name to "Sergey Lebedev", Scientists.reviewer to aErshov.gid), map::get)

        map[aLaypunov.gid] = aLaypunov
        map[aErshov.gid] = aErshov
        map[sLebedev.gid] = sLebedev
        val typing = Typing(aErshov, EagerQuery(), Scientist::class)
        assertEquals(3, typing.entities.size)
    }

    @Test
    fun `Test list ref traversing`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()
        val aLaypunov = AttachedEntity(gids.next(), mapOf<Attr<Any>, Any>(Scientists.name to "Aleksey Lyapunov"), map::get)
        val aErshov = AttachedEntity(gids.next(), mapOf(Scientists.name to "Andrey Ershov", Scientists.reviewer to aLaypunov.gid), map::get)
        val researchGroup = AttachedEntity(gids.next(), mapOf(ResearchGroups.members to listOf(aLaypunov.gid, aErshov.gid)), map::get)
        map[aLaypunov.gid] = aLaypunov
        map[aErshov.gid] = aErshov
        map[researchGroup.gid] = researchGroup

        val typing = Typing(researchGroup, EagerQuery(), ResearchGroup::class)
        assertEquals(3, typing.entities.size)
    }

    @Test
    fun `When typing with GraphQuery type T1, that has mandatory ref to type T2, mandatory props of T2 should be fetched too`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()
        val ru = AttachedEntity(gids.next(), mapOf<Attr<Any>, Any>(Countries.name to "Russia"), map::get)
        val nsk = AttachedEntity(gids.next(), mapOf(Regions.name to "Novosibirskaya obl.", Regions.country to ru), map::get)
        val nskCity = AttachedEntity(gids.next(), mapOf(Cities.name to "Novosibirsk", Cities.region to nsk), map::get)
        map[ru.gid] = ru
        map[nsk.gid] = nsk
        map[nskCity.gid] = nskCity

        val typing = Typing(nskCity, GraphQuery(City::class, emptyMap()), City::class)
        assertEquals(3, typing.entities.size)
    }

    @Test
    fun `Test instantiation of entity with missed fact`() {
        val gids = Gid(0, 0).nextGids()
        val sLebedevGid = gids.next()
        val aLaypunov = AttachedEntity(gids.next(), mapOf(/* no externalId */ Scientists.name to "Aleksey Lyapunov", Scientists.reviewer to sLebedevGid), { gid -> null })
        val typing = Typing(aLaypunov, EagerQuery(), Scientist::class)
        assertFailsWith<QBitException> {
            typing.instantiate(aLaypunov, Scientist::class)
        }
    }

    @Test
    fun `Test instantiation of self-referencing entity`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()
        val sLebedevGid = gids.next()

        val ru = AttachedEntity(gids.next(),
                mapOf(Countries.name to "Russia", Countries.population to 146_000_000),
                map::get)
        val sLebedev = AttachedEntity(sLebedevGid, mapOf(
                Scientists.name to "Sergey Lebedev",
                Scientists.extId to 1,
                Scientists.nicks to emptyList<String>(),
                Scientists.country to ru,
                Scientists.reviewer to sLebedevGid),
                map::get)

        map[sLebedev.gid] = sLebedev
        map[ru.gid] = ru

        val typing = Typing(sLebedev, EagerQuery(), Scientist::class)
        val typedLebedev = typing.instantiate(sLebedev, Scientist::class)
        assertEquals("Sergey Lebedev", typedLebedev.reviewer!!.name)
    }

    @Test
    fun `Test simple instantiation`() {
        val gids = Gid(0, 0).nextGids()
        val ru = AttachedEntity(gids.next(),
                mapOf(Countries.name to "Russia", Countries.population to 146_000_000),
                { _ -> null })
        val typing = Typing(ru, EagerQuery(), Country::class)
        val typedRu = typing.instantiate(ru, Country::class)
        assertEquals("Russia", typedRu.name)
    }

    @Test
    fun `Test simple graph instantiation`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()
        val ru = AttachedEntity(gids.next(),
                mapOf(Countries.name to "Russia", Countries.population to 146_000_000),
                map::get)
        val nsk = AttachedEntity(gids.next(),
                mapOf(Regions.name to "Novosibirskaya obl.", Regions.country to ru.gid),
                map::get)
        map[ru.gid] = ru
        val typing = Typing(nsk, EagerQuery(), Region::class)
        val typedNsk = typing.instantiate(nsk, Region::class)
        assertEquals("Novosibirskaya obl.", typedNsk.name)
        assertEquals("Russia", typedNsk.country.name)
    }

    @Test
    fun `Test instantiation of nullable value property without setter`() {
        val gids = Gid(0, 0).nextGids()

        val ru = AttachedEntity(gids.next(),
                mapOf(Countries.name to "Russia", Countries.population to 146_000_000),
                {_ -> null})
        val typing = Typing(ru, EagerQuery(), Country::class)
        val typedRu = typing.instantiate(ru, Country::class)
        assertEquals(146_000_000, typedRu.population)
    }

    @Test
    fun `Test instantiation of nullable ref property without setter`() {
        val gids = Gid(0, 0).nextGids()

        val er = AttachedEntity(gids.next(),
                mapOf(Papers.name to "ER-Model"),
                {_ -> null})
        val typing = Typing(er, EagerQuery(), Paper::class)
        val typedEr = typing.instantiate(er, Paper::class)
        assertEquals("ER-Model", typedEr.name)
        assertNull(typedEr.editor)
    }


    @Test
    fun `Test instantiation of nullable ref property with setter`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()
        val ru = AttachedEntity(gids.next(),
                mapOf(Countries.name to "Russia", Countries.population to 146_000_000),
                map::get)
        val aLaypunov = AttachedEntity(gids.next(), mapOf(Scientists.name to "Aleksey Lyapunov", Scientists.extId to 1, Scientists.nicks to emptyList<String>(), Scientists.country to ru), map::get)
        val aErshov = AttachedEntity(gids.next(), mapOf(Scientists.name to "Andrey Ershov", Scientists.reviewer to aLaypunov.gid, Scientists.extId to 2, Scientists.nicks to emptyList<String>(), Scientists.country to ru), map::get)
        map[aLaypunov.gid] = aLaypunov
        map[aErshov.gid] = aErshov
        map[ru.gid] = ru
        val typing = Typing(aErshov, EagerQuery(), Scientist::class)
        val typedErshov = typing.instantiate(aErshov, Scientist::class)
        assertEquals(aLaypunov[Scientists.name], typedErshov.reviewer?.name)
    }

    @Test
    fun `Test instantiation of ref cycle via nullable properties with setters`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()
        val sLebedevGid = gids.next()

        val ru = AttachedEntity(gids.next(),
                mapOf(Countries.name to "Russia", Countries.population to 146_000_000),
                map::get)
        val aLaypunov = AttachedEntity(gids.next(), mapOf(Scientists.name to "Aleksey Lyapunov", Scientists.reviewer to sLebedevGid, Scientists.extId to 1, Scientists.nicks to emptyList<String>(), Scientists.country to ru), map::get)
        val aErshov = AttachedEntity(gids.next(), mapOf(Scientists.name to "Andrey Ershov", Scientists.reviewer to aLaypunov.gid, Scientists.extId to 2, Scientists.nicks to emptyList<String>(), Scientists.country to ru), map::get)
        val sLebedev = AttachedEntity(sLebedevGid, mapOf(Scientists.name to "Sergey Lebedev", Scientists.reviewer to aErshov.gid, Scientists.extId to 3, Scientists.nicks to emptyList<String>(), Scientists.country to ru), map::get)

        map[aLaypunov.gid] = aLaypunov
        map[aErshov.gid] = aErshov
        map[sLebedev.gid] = sLebedev
        map[ru.gid] = ru
        val typing = Typing(aErshov, EagerQuery(), Scientist::class)
        val typedErshov = typing.instantiate(aErshov, Scientist::class)
        assertEquals(typedErshov, typedErshov.reviewer?.reviewer?.reviewer)
    }

    @Test
    fun `Test instantiation of entity without fact for optional property`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()

        val e = AttachedEntity(gids.next(), mapOf(NullableScalars.placeholder to 0), map::get)

        map[e.gid] = e
        val typing = Typing(e, EagerQuery(), NullableScalar::class)
        val ns = typing.instantiate(e, NullableScalar::class)
        assertEquals(0, ns.placeholder)
    }

    @Test
    fun `Test instantiation of entity with attribute for nullable mutable value property`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()

        val e = AttachedEntity(gids.next(), mapOf(NullableScalars.scalar to 1.toByte(), NullableScalars.placeholder to 1L), map::get)

        map[e.gid] = e
        val typing = Typing(e, EagerQuery(), NullableScalar::class)
        val ns = typing.instantiate(e, NullableScalar::class)
        assertEquals(1, ns.placeholder)
        assertEquals(1, ns.scalar)
    }

    @Test
    fun `Test instantiation of entity with not-null value for nullable values list attribute`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()

        val e = AttachedEntity(gids.next(), mapOf(NullableLists.lst to listOf(1.toByte()), NullableLists.placeholder to 1L), map::get)

        map[e.gid] = e
        val typing = Typing(e, EagerQuery(), NullableList::class)
        val ns = typing.instantiate(e, NullableList::class)
        assertEquals(listOf(1.toByte()), ns.lst)
    }

    @Test
    fun `Test instantiation of entity with not-null value for nullable ref attribute`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()

        val refGid = gids.next()
        val r = AttachedEntity(refGid, mapOf(IntEntities.int to 1), map::get)
        val e = AttachedEntity(gids.next(), mapOf(NullableRefs.ref to refGid, NullableRefs.placeholder to 1L), map::get)

        map[r.gid] = r
        map[e.gid] = e
        val typing = Typing(e, EagerQuery(), NullableRef::class)
        val ns = typing.instantiate(e, NullableRef::class)
        assertEquals(1, ns.ref?.int)
    }


    @Ignore
    @Test
    fun `Pulling entity of wrong type should fail with typing exception`() {
        val gids = Gid(0, 0).nextGids()
        val map = HashMap<Gid, StoredEntity>()

        val refGid = gids.next()
        val r = AttachedEntity(refGid, mapOf(IntEntities.int to 1), map::get)

        map[r.gid] = r
        val ex = assertFailsWith<QBitException> {
            val typing = Typing(r, EagerQuery(), NullableRef::class)
            val ns = typing.instantiate(r, NullableRef::class)
        }
        assertEquals("", ex.message)
    }

}