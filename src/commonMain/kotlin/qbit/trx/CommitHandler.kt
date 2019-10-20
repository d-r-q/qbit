package qbit.trx

import qbit.api.db.Db


internal interface CommitHandler {

    fun update(trxLog: TrxLog, newLog: TrxLog, newDb: Db)

}