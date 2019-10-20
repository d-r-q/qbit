package qbit.trx

import qbit.*
import qbit.collections.EmptyIterator
import qbit.db.Instance
import qbit.index.Db
import qbit.typing.destruct
import qbit.typing.gid
import qbit.model.Fact
import qbit.model.Gid
import qbit.model.toFacts


interface Trx {

    fun db(): Db

    fun <R : Any> persist(entityGraphRoot: R): WriteResult<R?>

    fun commit()

    fun rollback()

}

internal class QTrx(private val inst: Instance, private val trxLog: TrxLog, private var base: Db, private val commitHandler: CommitHandler) : Trx {

    private var curDb: Db? = null

    private val factsBuffer = ArrayList<Fact>()

    private val gids = Gid(inst.iid, inst.nextEid).nextGids()

    private var rollbacked = false

    override fun db() =
            (this.curDb ?: this.base)

    private val db: Db
        get() = db()

    override fun <R : Any> persist(entityGraphRoot: R): WriteResult<R?> {
        ensureReady()
        val facts = destruct(entityGraphRoot, db::attr, gids)
        val entities = facts.map { it.eid }
                .distinct()
                .mapNotNull { db.pull(it)?.toFacts()?.toList() }
                .map { it[0].eid to it }
                .toMap()
        val updatedFacts = facts.groupBy { it.eid }
                .filter { ue ->
                    ue.value != entities[ue.key]
                }
                .values
                .flatten()
        if (updatedFacts.isEmpty()) {
            return QbitWriteResult(entityGraphRoot, db)
        }
        validate(db, updatedFacts)
        factsBuffer.addAll(updatedFacts)
        curDb = db.with(updatedFacts)

        val res = if (entityGraphRoot.gid != null) {
            entityGraphRoot
        } else {
            facts.entityFacts[entityGraphRoot]?.get(0)?.eid?.let { db.pull(it, entityGraphRoot::class) }
        }
        return QbitWriteResult(res, curDb!!)
    }

    override fun commit() {
        ensureReady()
        if (factsBuffer.isEmpty()) {
            return
        }

        val instance = destruct(inst.copy(nextEid = gids.next().eid), curDb!!::attr, EmptyIterator)
        val newLog = trxLog.append(factsBuffer + instance)
        try {
            base = curDb!!.with(instance)
            commitHandler.update(trxLog, newLog, base)
            factsBuffer.clear()
        } catch (e: Throwable) {
            // todo clean up
            throw e
        }
    }

    override fun rollback() {
        rollbacked = true
        factsBuffer.clear()
        curDb = null
    }

    private fun ensureReady() {
        if (rollbacked) {
            throw QBitException("Transaction already has been rollbacked")
        }
    }

}