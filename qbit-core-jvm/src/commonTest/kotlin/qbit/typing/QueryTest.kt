package qbit.typing

import qbit.Scientists
import qbit.query.GraphQuery
import qbit.test.model.Scientist
import kotlin.test.Test
import kotlin.test.assertTrue


class QueryTest {

    @Test
    fun `Graph query should fetch refs for mandatory properties`() {
        val q = GraphQuery(Scientist::class, emptyMap())
        assertTrue(q.shouldFetch(Scientists.country))
    }

}