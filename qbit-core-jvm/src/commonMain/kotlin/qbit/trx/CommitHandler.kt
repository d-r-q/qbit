package qbit.trx

import qbit.index.InternalDb


internal interface CommitHandler {

    fun update(trxLog: TrxLog, newLog: TrxLog, newDb: InternalDb)

}