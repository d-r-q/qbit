package qbit

import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.Tweets.author
import qbit.Tweets.content
import qbit.Tweets.date
import qbit.Tweets.likes
import qbit.Users.lastLogin
import qbit.mapping.*
import qbit.model.*
import qbit.ns.Namespace
import qbit.storage.MemStorage
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val HHmm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

val tweetNs = Namespace.of("demo", "tweet")
val userNs = Namespace.of("demo", "user")

object Users {
    val name = ScalarAttr(userNs["name"], QString, unique = true)
    val lastLogin = ScalarAttr(userNs["last_login"], QInstant)
}

object Tweets {
    val content = ScalarAttr(tweetNs["content"], QString)
    val author = RefAttr(tweetNs["author"])
    val date = ScalarAttr(tweetNs["date"], QZonedDateTime)
    val likes = RefListAttr(tweetNs["likes"])
}

class User<E : EID?>(entity: Entity<E>) : TypedEntity<E>(entity) {

    var name: String by AttrDelegate(Users.name)

    val last_login: Instant by AttrDelegate(lastLogin)

}

class Demo {

    @Test
    fun main() {

        val conn = qbit(MemStorage())
        conn.persist(Users.name, lastLogin, content, author, date, likes)

        val user = Entity(Users.name eq "@azhidkov", lastLogin eq Instant.now())
        val tweet = Entity(content eq "Hello @HackDay",
                author eq user,
                date eq ZonedDateTime.now())
        val storedUser = conn.persist(tweet).createdEntities.getValue(user)
        conn.persist(storedUser.with(lastLogin, Instant.now()))

        val sTweet = conn.db.query(attrIs(content, "Hello @HackDay")).first()
        println("${sTweet[date].format(HHmm)} | ${sTweet[author][Users.name]}: ${sTweet[content]}")

        val cris = Entity(Users.name eq "@cris", lastLogin eq Instant.now())
        var nTweet: StoredEntity = sTweet.with(content eq "Array with works", likes eq listOf(storedUser, cris))
        nTweet = conn.persist(cris, nTweet).persistedEntities[1]

        println(nTweet[content])
        println(nTweet[likes].map { it[Users.name] })
        val likedUsers = nTweet[likes]
        val users = likedUsers.filterIsInstance<QRoEntity<EID>>().map { typify<EID, User<EID>>(it) }
        println(users.map { p -> "${p.name}: ${p.last_login}" })

        users[0].name = "@reflection_rulezz"
        conn.persist(users[0])
        assertEquals("@reflection_rulezz", conn.db.pullAs<User<EID>>(users[0].eid)!!.name)

    }
}