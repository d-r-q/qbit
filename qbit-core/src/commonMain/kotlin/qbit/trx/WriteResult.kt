package qbit.trx

import qbit.api.db.Db
import qbit.api.db.WriteResult

internal data class QbitWriteResult<R>(
        override val persisted: R,
        override val db: Db
) : WriteResult<R>()

