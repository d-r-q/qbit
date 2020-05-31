package qbit.factoring

import qbit.api.QBitException
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.api.model.Entity
import qbit.api.model.Tombstone
import qbit.api.model.impl.DetachedEntity
import qbit.collections.IdentityMap

@Suppress("UNCHECKED_CAST")
internal data class EntityBuilder(
    internal var gid: Gid? = null,
    private val values: HashMap<AttrName, Any> = HashMap()
) {

    fun setValue(attr: AttrName, v: Any) {
        if (attr.name in setOf("id", "gid") && v is Long) {
            gid = Gid(v)
        } else if (attr.name in setOf("id", "gid") && v is Gid) {
            gid = v
        } else {
            values[attr] = v
        }
    }

    fun toEntity(schema: (String) -> Attr<*>?, resolve: (Any) -> Gid): DetachedEntity {
        check(gid != null) { "Entity $values has no gid"}
        val attrValues: Map<Attr<*>, Any> = values
            .mapKeys { schema(it.key.asString()) ?: throw QBitException("Attr for ${it.key.asString()} not found") }
            .mapValues { resolveRefs(it.value, resolve) }
        return DetachedEntity(gid!!, attrValues)
    }

    private fun resolveRefs(attrVallue: Any, resolve: (Any) -> Gid): Any {
        return when {
            attrVallue is Ref -> resolve(attrVallue.obj)
            attrVallue is List<*> && attrVallue.firstOrNull() is Ref -> (attrVallue as List<Ref>).map { resolve(it.obj) }
            else -> attrVallue
        }
    }

}

internal data class Ref(val obj: Any)

data class AttrName(val className: String, val name: String) {

    fun asString(): String {
        return "${className.substringAfterLast(".")}/$name"
    }

}

internal data class Pointer(val obj: Any, val attr: AttrName)

internal class EntityGraph {

    val builders = IdentityMap<Any, EntityBuilder>()

    private val tombstones = HashSet<Tombstone>()

    fun setAttributeValue(p: Pointer, v: Any) {
        builders.getValue(p.obj).setValue(p.attr, v)
    }

    fun resolveRefs(schema: (String) -> Attr<*>?, gids: Iterator<Gid>): IdentityMap<Any, Entity> {
        assignGids(gids)
        val gid2builder = deduplicateEntityStates()
        val entries: List<Pair<Any, Entity>> = builders
            .map { (ref, builder) -> ref to (gid2builder[builder.gid] ?: throw QBitException("Could not find builder for ref $ref via ${builder.gid}")) }
            .map { (ref, builder) ->
                ref to builder.toEntity(schema) { r ->
                    val refereeBuilder = builders[r] ?: throw AssertionError("Unexpected ref: $r")
                    refereeBuilder.gid ?: throw AssertionError("Builder does not have gid after assignment")
                }
            } + tombstones.map { it to it }
        return IdentityMap(*entries.toTypedArray())
    }

    private fun assignGids(gids: Iterator<Gid>) {
        builders.values
            .filter { it.gid == null }
            .forEach { it.gid = gids.next() }
    }

    private fun deduplicateEntityStates(): Map<Gid, EntityBuilder> {
        val uniqueBuilders = builders.values.groupBy { it.gid }
        return uniqueBuilders.map { (gid, builders) ->
            check(gid != null) { "Gid for builders $builders is null" }
            val states = builders.toSet()
            if (states.size > 1) {
                throw QBitException("Entity $gid has several different states to store: $states")
            }
            gid to builders.first()
        }.toMap()
    }

    fun addTombstone(tombstone: Tombstone) {
        tombstones += tombstone
    }

    fun addIfMissing(obj: Any) {
        if (obj !in builders) {
            builders[obj] = EntityBuilder()
        }
    }

}
