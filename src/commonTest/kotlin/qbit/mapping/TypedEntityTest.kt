package qbit.mapping

import qbit.assertArrayEquals
import qbit.mapping.GenericEntities.meta
import qbit.mapping.GenericEntities.ref
import qbit.mapping.GenericEntities.refs
import qbit.model.*
import qbit.ns.root
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


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

object GenericEntities {

    val meta = ScalarAttr(root["meta"], QString)

    val ref = RefAttr(root["ref"])

    val refs = RefListAttr(root["refs"])

}

fun User(name: String) = User(Entity(Users.name eq name))
class User<E : EID?>(entity: Entity<E>) : TypedEntity<E>(entity) {

    var name: String by AttrDelegate(Users.name)

    var pass: String? by AttrDelegate(Users.pass)

    var emails: List<String>? by ListAttrDelegate(Users.emails)

}

fun Post(post: String, user: User<*>) = Post(Entity(Posts.user eq user, Posts.post eq post))
class Post<E : EID?>(entity: Entity<E>) : TypedEntity<E>(entity) {

    var user: User<*> by RefAttrDelegate(Posts.user)

    var replyTo: User<*>? by RefAttrDelegate(Posts.replyTo)

    var mentions: List<User<*>>? by RefListAttrDelegate(Posts.mentions)

    val post: String by AttrDelegate(Posts.post)

}

class GenericEntity<T, E : EID?>(val value: T?, entity: Entity<E>) : TypedEntity<E>(entity) {

    constructor(entity: Entity<E>) : this(null, entity)

    var meta: String by AttrDelegate(GenericEntities.meta)

    var ref: GenericEntity<T, E> by RefAttrDelegate(GenericEntities.ref)

    var refs: List<GenericEntity<T, E>> by RefListAttrDelegate(GenericEntities.refs)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GenericEntity<*, *>) return false

        if (value != other.value) return false

        if (entity != other.entity) return false

        return true
    }

    override fun hashCode(): Int {
        return entity.hashCode()
    }

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
    fun testGetTyped() {
        val user = User("test")
        val post = Post("post", user)
        assertEquals(user, post.user)
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
        assertEquals(user, typify(user, User::class))
    }

    @Test
    fun testTypifyTypedReified() {
        val user = User("user1")
        val typified: User<EID?> = typify(user)
        assertEquals(user, typified)
    }

    @Test
    fun testGenerifiedEntity() {
        val ge1 = GenericEntity(1, Entity(meta eq "meta1"))
        assertEquals("meta1", ge1.meta)
        ge1.meta = "meta1.2"
        assertEquals("meta1.2", ge1.meta)
        val ge2 = GenericEntity(2, Entity(ref eq ge1))
        assertEquals(ge1, ge2.ref)
        ge2.ref = ge2
        assertEquals(ge2, ge2.ref)

        val ge4 = Entity(refs eq listOf(ge1, ge2, Entity(meta eq "meta4")))
        val ge3 = GenericEntity(3, Entity(meta eq "meta3", refs eq listOf(ge1, ge4)))
        assertEquals(GenericEntity<Int, EID?>(null, ge4), ge3.refs[1])

        ge3.refs = listOf(ge2, ge1)
        assertArrayEquals(listOf(ge2, ge1).toTypedArray(), ge3.refs.toTypedArray())
    }
}
