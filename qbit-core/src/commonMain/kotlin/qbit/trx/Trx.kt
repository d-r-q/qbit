package qbit.trx

import qbit.api.QBitException
import qbit.api.db.Trx
import qbit.api.db.WriteResult
import qbit.api.gid.Gid
import qbit.api.model.*
import qbit.api.system.Instance
import qbit.factoring.Factor
import qbit.index.InternalDb
import qbit.platform.collections.EmptyIterator


internal class QTrx(
    private val inst: Instance, private val trxLog: TrxLog, private var base: InternalDb,
    private val commitHandler: CommitHandler, private val factor: Factor,
    private val gids: GidSequence
) : Trx() {

    private var curDb: InternalDb = base

    private val factsBuffer = ArrayList<Eav>()

    private var rollbacked = false

    override fun db() =
        this.curDb

    override fun <R : Any> persist(entityGraphRoot: R): WriteResult<R?> {
        ensureReady()
        val facts = factor(entityGraphRoot, curDb::attr, gids)
        val entities = facts.map { it.gid }
            .distinct()
            .mapNotNull { curDb.pullEntity(it)?.toFacts()?.toList() }
            .map { it[0].gid to it }
            .toMap()
        val updatedFacts = facts.groupBy { it.gid }
            .filter { ue ->
                ue.value.sortedWith(EavComparator) != entities[ue.key]?.sortedWith(EavComparator)
            }
            .values
            .flatten()
        if (updatedFacts.isEmpty()) {
            return QbitWriteResult(entityGraphRoot, curDb)
        }
        validate(curDb, updatedFacts)
        factsBuffer.addAll(updatedFacts)
        curDb = curDb.with(updatedFacts)

        val res = if (facts.entityFacts[entityGraphRoot]!!.firstOrNull()?.gid in entities) {
            entityGraphRoot
        } else {
            facts.entityFacts[entityGraphRoot]?.get(0)?.gid?.let { curDb.pull(it, entityGraphRoot::class) }
        }
        return QbitWriteResult(res, curDb)
    }

    override suspend fun commit() {
        ensureReady()
        if (factsBuffer.isEmpty()) {
            return
        }

        val instance = factor(inst.copy(nextEid = gids.next().eid), curDb::attr, EmptyIterator)
        val newLog = trxLog.append(factsBuffer + instance)
        try {
            commitHandler.update(trxLog, base, newLog, curDb.with(instance))
            factsBuffer.clear()
        } catch (e: Throwable) {
            // todo clean up
            throw e
        }
    }

    override fun rollback() {
        rollbacked = true
        factsBuffer.clear()
        curDb = base
    }

    private fun ensureReady() {
        if (rollbacked) {
            throw QBitException("Transaction already has been rollbacked")
        }
    }

}

fun Entity.toFacts(): Collection<Eav> =
    this.entries.flatMap { (attr: Attr<Any>, value) ->
        val type = DataType.ofCode(attr.type)!!
        @Suppress("UNCHECKED_CAST")
        when {
            type.value() && !(attr.list || DataType.ofCode(attr.type)!!.isRegister()) -> listOf(valToFacts(gid, attr, value))
            type.value() && (attr.list || DataType.ofCode(attr.type)!!.isRegister()) -> listToFacts(gid, attr, value as List<Any>)
            type.ref() && !(attr.list || DataType.ofCode(attr.type)!!.isRegister()) -> listOf(refToFacts(gid, attr, value))
            type.ref() && (attr.list || DataType.ofCode(attr.type)!!.isRegister()) -> refListToFacts(gid, attr, value as List<Any>)
            else -> throw AssertionError("Unexpected attr kind: $attr")
        }
    }

private fun <T : Any> valToFacts(eid: Gid, attr: Attr<T>, value: T) =
    Eav(eid, attr, value)

private fun refToFacts(eid: Gid, attr: Attr<Any>, value: Any) =
    Eav(eid, attr, eidOf(value)!!)

private fun listToFacts(eid: Gid, attr: Attr<*>, value: List<Any>) =
    value.map { Eav(eid, attr, it) }

private fun refListToFacts(eid: Gid, attr: Attr<*>, value: List<Any>) =
    value.map { Eav(eid, attr, eidOf(it)!!) }

private fun eidOf(a: Any): Gid? =
    when (a) {
        is Entity -> a.gid
        is Gid -> a
        else -> null
    }