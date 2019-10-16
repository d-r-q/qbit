package qbit

import qbit.trx.InternalConn
import qbit.trx.Trx
import qbit.trx.TrxLog

internal class FakeConn : InternalConn {

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

    override fun persist(e: Any): Db {
        TODO("not implemented")
    }

    override val head: Hash
        get() = TODO("not implemented")

    override fun update(trxLog: TrxLog, newLog: TrxLog) {
        updatesCalls++
    }

}
