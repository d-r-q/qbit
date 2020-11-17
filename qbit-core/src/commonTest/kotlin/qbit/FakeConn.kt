package qbit

import qbit.api.db.Conn
import qbit.api.db.Db
import qbit.api.db.Trx
import qbit.api.db.WriteResult
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.index.InternalDb
import qbit.trx.CommitHandler
import qbit.trx.TrxLog

internal class FakeConn : Conn(), CommitHandler {

    var updatesCalls = 0

    override val dbUuid: DbUuid
        get() = TODO("not implemented")

    override fun db(): Db {
        TODO("not implemented")
    }

    override fun db(body: (Db) -> Unit) {
        TODO("not implemented")
    }

    override fun trx(): Trx {
        TODO("not implemented")
    }

    override suspend fun <R : Any> persist(e: R): WriteResult<R?> {
        TODO("not implemented")
    }

    override val head: Hash
        get() = TODO("not implemented")

    override suspend fun update(trxLog: TrxLog, newLog: TrxLog, newDb: InternalDb) {
        updatesCalls++
    }

}
