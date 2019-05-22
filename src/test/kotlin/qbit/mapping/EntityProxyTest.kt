package qbit.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import qbit.Entity
import qbit.QString
import qbit.ns.root
import qbit.schema.ScalarAttr
import qbit.schema.eq


val name = ScalarAttr(root["name"], QString)

fun User(uname: String) = proxy<User>(Entity(name eq uname))
interface User {

    var name: String

    var pass: String

}

class EntityProxyTest {

    @Test
    fun testMutableProxy() {
        val user = User("test1")
        user.name = "test2"
        assertEquals("test2", user.name)
    }

    @Test
    fun testGetNotSetAttr() {
        val user = User("test1")
        assertNull(user.pass)
    }

}