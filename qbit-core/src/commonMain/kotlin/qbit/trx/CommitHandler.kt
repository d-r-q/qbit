package qbit.trx

import qbit.index.InternalDb


interface CommitHandler {

    suspend fun update(trxLog: TrxLog, newLog: TrxLog, newDb: InternalDb)

}