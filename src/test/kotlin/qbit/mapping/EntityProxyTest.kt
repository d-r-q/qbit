package qbit.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import qbit.Entity
import qbit.QString
import qbit.ns.root
import qbit.schema.RefAttr
import qbit.schema.ScalarAttr
import qbit.schema.eq


val name = ScalarAttr(root["name"], QString)
val post = ScalarAttr(root["post"], QString)
val user = RefAttr(root["user"])

fun User(uname: String) = proxy<User>(Entity(name eq uname))
interface User : EntityHolder {

    var name: String

    var pass: String

}

fun Post(ppost: String, puser: Entity) = proxy<Post>(Entity(post eq ppost, user eq puser))
interface Post {

    var post: String

    var user: User

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

    @Test
    fun testGetEntity() {
        val user = User("test")
        val post = Post("post", user.entity())
        assertEquals("test", post.user.name)
    }

}