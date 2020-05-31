package qbit.api.db

import qbit.api.model.Hash
import qbit.api.system.DbUuid

abstract class Conn() {

    abstract val dbUuid: DbUuid

    abstract fun db(): Db

    abstract fun db(body: (Db) -> Unit)

    abstract fun trx(): Trx

    abstract suspend fun <R : Any> persist(e: R): WriteResult<R?>

    abstract val head: Hash

}