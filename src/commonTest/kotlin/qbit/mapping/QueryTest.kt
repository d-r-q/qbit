package qbit.mapping

import qbit.User
import qbit.Users
import kotlin.test.Test
import kotlin.test.assertTrue


class QueryTest {

    @Test
    fun `Graph query should fetch refs for mandatory properties`() {
        val q = GraphQuery(User::class, emptyMap())
        assertTrue(q.shouldFetch(Users.country))
    }

}