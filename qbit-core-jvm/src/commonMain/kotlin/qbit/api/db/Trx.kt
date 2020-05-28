package qbit.api.db

abstract class Trx internal constructor() {

    abstract fun db(): Db

    abstract fun <R : Any> persist(entityGraphRoot: R): WriteResult<R?>

    abstract suspend fun commit()

    abstract fun rollback()

}