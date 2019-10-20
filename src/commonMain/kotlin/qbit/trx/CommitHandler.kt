package qbit.trx

import qbit.index.Db


internal interface CommitHandler {

    fun update(trxLog: TrxLog, newLog: TrxLog, newDb: Db)

}