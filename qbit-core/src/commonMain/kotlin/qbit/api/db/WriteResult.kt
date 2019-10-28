package qbit.api.db

abstract class WriteResult<R> internal constructor() {

    abstract val persisted: R

    abstract val db: Db

    abstract operator fun component1(): R

}