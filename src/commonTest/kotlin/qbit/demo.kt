package qbit

import qbit.Tweets.author
import qbit.Tweets.content
import qbit.Tweets.date
import qbit.Tweets.likes
import qbit.Users.lastLogin
import qbit.mapping.*
import qbit.model.*
import qbit.ns.Namespace
import qbit.platform.*
import qbit.storage.MemStorage
import kotlin.test.Test
import kotlin.test.assertEquals

val HHmm: DateTimeFormatter = DateTimeFormatters.ofPattern("HH:mm")

val tweetNs = Namespace.of("demo", "tweet")
val userNs = Namespace.of("demo", "user")

object Users {
    val name = ScalarAttr(userNs["name"], QString, unique = true)
    val lastLogin = ScalarAttr(userNs["lastLogin"], QInstant)
}

object Tweets {
    val content = ScalarAttr(tweetNs["content"], QString)
    val author = RefAttr(tweetNs["author"])
    val date = ScalarAttr(tweetNs["date"], QZonedDateTime)
    val likes = RefListAttr(tweetNs["likes"])
}

class User<E : EID?>(entity: Entity<E>) : TypedEntity<E>(entity) {

    var name: String by AttrDelegate(Users.name)

    val lastLogin: Instant by AttrDelegate(Users.lastLogin)

}

class Demo {

    @Test
    fun main() {

        // open connection
        val conn = qbit(MemStorage())

        // create schema
        conn.persist(Users.name, lastLogin, content, author, date, likes)

        // store data
        val user = Entity(Users.name eq "@azhidkov", lastLogin eq Instants.now())
        val tweet = Entity(content eq "Hello @HackDay",
                author eq user,
                date eq ZonedDateTimes.now())

        // updateData
        val storedUser = conn.persist(tweet).createdEntities.getValue(user)
        conn.persist(storedUser.with(lastLogin, Instants.now()))

        // query data
        val storedTweet = conn.db.query(attrIs(content, "Hello @HackDay")).first()
        println("${storedTweet[date].format(HHmm)} | ${storedTweet[author][Users.name]}: ${storedTweet[content]}")

        // store list
        val cris = Entity(Users.name eq "@cris", lastLogin eq Instants.now())
        var updatedTweet: StoredEntity = storedTweet.with(content eq "List with works", likes eq listOf(storedUser, cris))
        updatedTweet = conn.persist(cris, updatedTweet).persistedEntities[1]

        println(updatedTweet[content])
        println(updatedTweet[likes].map { it[Users.name] })

        // Typed API
        val users: List<User<EID>> = updatedTweet.getAs(likes)

        // typed access
        println(users.map { p -> "${p.name}: ${p.lastLogin}" })

        // typed modification
        users[0].name = "@reflection_rulezz"
        conn.persist(users[0])
        assertEquals("@reflection_rulezz", conn.db.pullAs<User<EID>>(users[0].eid)!!.name)

    }
}