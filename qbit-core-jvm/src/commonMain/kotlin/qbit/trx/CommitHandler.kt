package qbit.trx

import qbit.index.InternalDb


internal interface CommitHandler {

    suspend fun update(trxLog: TrxLog, newLog: TrxLog, newDb: InternalDb)

}