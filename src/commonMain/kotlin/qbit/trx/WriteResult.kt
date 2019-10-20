package qbit.trx

interface WriteResult<R> {

    val persisted: R

    val db: Db

    operator fun component1(): R

}

internal data class QbitWriteResult<R>(
        override val persisted: R,
        override val db: Db
) : WriteResult<R>

