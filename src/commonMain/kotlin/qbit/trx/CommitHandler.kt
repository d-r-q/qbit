package qbit.trx


internal interface CommitHandler {

    fun update(trxLog: TrxLog, newLog: TrxLog, newDb: Db)

}