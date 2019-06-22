package qbit.mapping

import org.junit.Assert.*
import org.junit.Test
import qbit.model.*
import qbit.ns.root


object Users {

    val name = ScalarAttr(root["name"], QString)

    val pass = ScalarAttr(root["pass"], QString)

    val emails = ListAttr(root["emails"], QString)

}

object Posts {

    val user = RefAttr(root["user"])

    val replyTo = RefAttr(root["replyTo"])

    val mentions = RefListAttr(root["mentions"])

    val post = ScalarAttr(root["post"], QString)

}

fun User(name: String) = User(Entity(Users.name eq name))
class User(entity: MutableEntitiable) : TypedEntity(entity) {

    var name: String by AttrDelegate(Users.name)

    var pass: String? by AttrDelegate(Users.pass)

    var emails: List<String>? by ListAttrDelegate(Users.emails)

}

fun Post(post: String, user: User) = Post(Entity(Posts.user eq user, Posts.post eq post))
class Post(entity: MutableEntitiable) : TypedEntity(entity) {

    var user: User by RefAttrDelegate(Posts.user)

    var replyTo: User? by RefAttrDelegate(Posts.replyTo)

    var mentions: List<User>? by RefListAttrDelegate(Posts.mentions)

    val post: String by AttrDelegate(Posts.post)

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
        val post = Post("post", user)
        assertEquals("test", post.user.name)
    }

    @Test
    fun testSetEntity() {
        val user1 = User("test1")
        val user2 = User("test3")
        val post = Post("post", user1)
        post.user = user2
        assertEquals("test3", post.user.name)
    }

    @Test
    fun testSetNullScalar() {
        val user = User("name")
        assertNull(user.pass)
        user.pass = "pass"
        assertEquals("pass", user.pass)
        user.pass = null
        assertNull(user.pass)
    }

    @Test
    fun testSetNullList() {
        val user = User("name")
        assertNull(user.emails)
        user.emails = listOf("test@ya.ru", "test@google.com")
        assertArrayEquals(listOf("test@ya.ru", "test@google.com").toTypedArray(), user.emails?.toTypedArray())
        user.emails = null
        assertNull(user.emails)
    }

    @Test
    fun testSetNullRef() {
        val user = User("user")
        val replyTo = User("replyTo")
        val post = Post("post", user)
        assertNull(post.replyTo)
        post.replyTo = replyTo
        assertEquals("replyTo", post.replyTo?.name)
        post.replyTo = null
        assertNull(post.replyTo)
    }

    @Test
    fun testSetNullRefList() {
        val user1 = User("user1")
        val user2 = User("user2")
        val user3 = User("user3")
        val post = Post("post", user1)
        assertNull(post.mentions)
        post.mentions = listOf(user2, user3)
        assertArrayEquals(listOf(user2, user3).toTypedArray(), post.mentions?.toTypedArray())
        post.mentions = null
        assertNull(post.mentions)
    }

    @Test
    fun testTypifyTyped() {
        val user = User("user1")
        assertEquals(user, typify(user, User::class.java))
    }

    @Test
    fun testTypifyTypedReified() {
        val user = User("user1")
        val typified: User = typify(user)
        assertEquals(user, typified)
    }

    // todo: test generified typed entity
}
