package qbit.index

import qbit.model.*
import qbit.model.Attrs.list
import qbit.model.Attrs.name
import qbit.model.Attrs.type
import qbit.model.Attrs.unique
import qbit.platform.WeakHashMap
import qbit.platform.set
import qbit.query.*
import qbit.typing.Typing
import kotlin.reflect.KClass

inline fun <reified R : Any> Db.pullT(eid: Gid): R? {
    return this.pull(eid, R::class)
}

inline fun <reified R : Any> Db.pullT(eid: Long): R? {
    return this.pull(Gid(eid), R::class)
}

inline fun <reified R : Any> Db.queryT(vararg preds: QueryPred, fetch: Fetch = Lazy): Sequence<R> =
        this.queryGids(*preds).map { this.pull(it, R::class, fetch)!! }

internal class IndexDb(internal val index: Index) : Db {

    private val schema = loadAttrs(index)

    private val notFound = AttachedEntity(Gid(0, 0), emptyList(), this::pull)

    private val entityCache = WeakHashMap<Gid, StoredEntity>()

    private val dcCache = WeakHashMap<Entity, Any>()

    override fun with(facts: Iterable<Eav>): Db {
        return IndexDb(index.addFacts(facts))
    }

    override fun pull(gid: Gid): StoredEntity? {
        val cached = entityCache[gid]
        if (cached === notFound) {
            return null
        } else if (cached != null) {
            return cached
        }

        val rawEntity = index.entityById(gid)
        if (rawEntity == null) {
            entityCache[gid] = notFound
            return null
        }
        val attrValues = rawEntity.entries.map {
            val attr = schema[it.key]
            require(attr != null) { "There is no attribute with name ${it.key}" }
            require(attr.list || it.value.size == 1) { "Corrupted ${attr.name} of $gid - it is scalar, but multiple values has been found: ${it.value}" }
            attr to if (attr.list) it.value else it.value[0]
        }
        val entity = AttachedEntity(gid, attrValues, this::pull)
        entityCache[gid] = entity
        return entity
    }

    override fun <R : Any> pull(gid: Gid, type: KClass<R>, fetch: Fetch): R? {
        val entity = pull(gid) ?: return null
        val cached = dcCache[entity]
        if (cached === notFound) {
            return null
        } else if (cached != null) {
            @Suppress("UNCHECKED_CAST")
            return cached as R
        }

        val query = when (fetch) {
            Lazy -> GraphQuery(type, emptyMap())
            Eager -> EagerQuery<R>()
        }

        val typing = Typing(entity, query, type)
        val dc = typing.instantiate(entity, type)
        dcCache[entity] = dc
        return dc
    }

    override fun query(vararg preds: QueryPred): Sequence<Entity> {
        return queryGids(*preds).map { pull(it)!! }
    }

    override fun queryGids(vararg preds: QueryPred): Sequence<Gid> {
        if (preds.isEmpty()) {
            return index.entities.keys.asSequence()
        }

        // filters data + base sequence data
        val arrayOfFalse = { Array(preds.size) { false } }

        val base = index.eidsByPred(preds[0])
        val filters: List<Iterator<Gid>> = preds.drop(1).map { index.eidsByPred(it).iterator() }

        // eids, that were fetched from filtering sequences while candidates checking
        val seenEids = hashMapOf<Gid, Array<Boolean>>()

        return base // iterate candidates and check that candidate matches filters
                .filter { cEid ->
                    val filtersSeenEid = seenEids.getOrPut(cEid, arrayOfFalse)

                    if (filtersSeenEid.all { it }) {
                        // the eid already has been returned, so skip duplicate
                        return@filter false
                    }

                    // seek all filters for the eid
                    filtersSeenEid
                            .take(filters.size)
                            .withIndex()
                            .filterNot { f -> f.value } // for filters for which the eid was not seen yet...
                            .forEach { f ->
                                // seek it for the eid
                                val matches = filters[f.index].asSequence() // "enrich" iterator with takeWhile
                                        .onEach { fEid ->
                                            // remember what we saw in process of...
                                            seenEids.getOrPut(fEid, arrayOfFalse)[f.index] = true
                                        }
                                        .any { fEid -> fEid == cEid } // searching of the eid

                                // if the eid has been found, then it has been seen in the filter
                                seenEids.getValue(cEid)[f.index] = matches
                            }

                    // candidate is matches if it has been seen in all filters
                    filtersSeenEid[filtersSeenEid.size - 1] = true // mark, that eid was seen for base sequence
                    filtersSeenEid.all { it }
                }
    }

    override fun attr(attr: String): Attr<Any>? = schema[attr]

    companion object {

        private fun loadAttrs(index: Index): Map<String, Attr<Any>> {
            val attrEids = index.eidsByPred(hasAttr(name))
            @Suppress("UNCHECKED_CAST")
            val attrFacts = attrEids
                    .map {
                        val e: Map<String, List<Any>> = index.entityById(it)!!
                        val name = e.getValue(name.name)[0] as String
                        val type = e.getValue(type.name)[0] as Byte
                        val unique = e[unique.name]?.firstOrNull() as? Boolean ?: false
                        val list = e[list.name]?.firstOrNull() as? Boolean ?: false
                        val attr = Attr<Any>(it, name, type, unique, list)
                        (name to attr)
                    }
            return attrFacts.toMap()
        }
    }

}