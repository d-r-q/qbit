package qbit

import qbit.db.Conn
import qbit.db.DbUuid
import qbit.index.Db
import qbit.trx.*

internal class FakeConn : Conn, CommitHandler {

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

    override fun <R : Any> persist(e: R): WriteResult<R?> {
        TODO("not implemented")
    }

    override val head: Hash
        get() = TODO("not implemented")

    override fun update(trxLog: TrxLog, newLog: TrxLog, newDb: Db) {
        updatesCalls++
    }

}
