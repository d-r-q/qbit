package qbit.index

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.serialization.modules.SerializersModule
import qbit.api.Attrs.list
import qbit.api.Attrs.name
import qbit.api.Attrs.type
import qbit.api.Attrs.unique
import qbit.api.db.Fetch
import qbit.api.db.QueryPred
import qbit.api.db.hasAttr
import qbit.api.gid.Gid
import qbit.api.model.*
import qbit.api.model.impl.AttachedEntity
import qbit.collections.LimitedPersistentMap
import qbit.trx.deoperationalize
import qbit.typing.typify
import kotlin.reflect.KClass

class IndexDb(
    internal val index: Index,
    private val serialModule: SerializersModule
) : InternalDb() {

    private val schema = loadAttrs(index)

    private val notFound = AttachedEntity(Gid(0, 0), emptyList(), this::pullEntity)

    private val entityCache = atomic<LimitedPersistentMap<Gid, StoredEntity>>(LimitedPersistentMap(1024))

    private val dataClassesCache = atomic<LimitedPersistentMap<Entity, Any>>(LimitedPersistentMap(1024))

    override fun with(facts: Iterable<Eav>): IndexDb {
        return IndexDb(index.addFacts(deoperationalize(this, facts.toList()), this::attr), serialModule)
    }

    override fun pullEntity(gid: Gid): StoredEntity? {
        val cached = entityCache.value[gid]
        if (cached === notFound) {
            return null
        } else if (cached != null) {
            return cached
        }

        val rawEntity = index.entityById(gid)
        if (rawEntity == null) {
            entityCache.update { it.put(gid, notFound) }
            return null
        }
        val attrValues = rawEntity.entries.map {
            val attr = schema[it.key]
            require(attr != null) { "There is no attribute with name ${it.key}" }
            require(attr.list || it.value.size == 1) { "Corrupted ${attr.name} of $gid - it is scalar, but multiple values has been found: ${it.value}" }
            val value =
                if (attr.list) it.value.map { e -> fixNumberType(attr, e) }
                else fixNumberType(attr, it.value[0])
            attr to value
        }
        val entity = AttachedEntity(gid, attrValues, this::pullEntity)
        entityCache.update { it.put(gid, entity) }
        return entity
    }

    // see https://github.com/d-r-q/qbit/issues/114, https://github.com/d-r-q/qbit/issues/132
    private fun fixNumberType(attr: Attr<Any>, value: Any) =
        when (attr.type) {
            QByte.code -> (value as Number).toByte()
            QInt.code -> (value as Number).toInt()
            else -> value
        }

    override fun <R : Any> pull(gid: Gid, type: KClass<R>, fetch: Fetch): R? {
        val entity = pullEntity(gid) ?: return null
        val cached = dataClassesCache.value[entity]
        if (cached === notFound) {
            return null
        } else if (cached != null) {
            @Suppress("UNCHECKED_CAST")
            return cached as R
        }

        val dc = typify(schema::get, entity, type, serialModule)
        dataClassesCache.update { it.put(entity, dc) }
        return dc
    }

    override fun query(vararg preds: QueryPred): Sequence<Entity> {
        return queryGids(*preds).map { pullEntity(it)!! }
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
                    val type = (e.getValue(type.name)[0] as? Long)?.toByte() ?: e.getValue(type.name)[0] as Byte
                    val unique = e[unique.name]?.firstOrNull() as? Boolean ?: false
                    val list = e[list.name]?.firstOrNull() as? Boolean ?: false
                    val attr = Attr<Any>(it, name, type, unique, list)
                    (name to attr)
                }
            return attrFacts.toMap()
        }
    }

}