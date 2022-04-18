package qbit.trx

import qbit.index.InternalDb


internal interface CommitHandler {

    suspend fun update(trxLog: TrxLog, baseDb: InternalDb, newLog: TrxLog, newDb: InternalDb)

}