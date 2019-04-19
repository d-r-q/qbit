package qbit

import qbit.mapping.EntityHolder
import qbit.mapping.proxy
import qbit.mapping.pullAs
import qbit.ns.Namespace
import qbit.schema.*
import qbit.storage.MemStorage
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val HHmm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

interface User : Entity, EntityHolder {

    fun name(): String
    fun name(newName: String): User

    fun last_login(): Instant

}

fun main() {
    val tweetNs = Namespace.of("demo", "tweet")
    val userNs = Namespace.of("demo", "user")

    val name = ScalarAttr(userNs["name"], QString, unique = true)
    val lastLogin = ScalarAttr(userNs["last_login"], QInstant)

    val content = ScalarAttr(tweetNs["content"], QString)
    val author = RefAttr(tweetNs["author"])
    val date = ScalarAttr(tweetNs["date"], QZonedDateTime)
    val likes = RefListAttr(tweetNs["likes"])

    val conn = qbit(MemStorage())
    conn.persist(name, lastLogin, content, author, date, likes)

    val user = Entity(name eq "@azhidkov", lastLogin eq Instant.now())
    val tweet = Entity(content eq "Hello @HackDay",
            author eq user,
            date eq ZonedDateTime.now())
    val storedUser = conn.persist(tweet).createdEntities.getValue(user)
    conn.persist(storedUser.set(lastLogin, Instant.now()))

    val sTweet = conn.db.query(attrIs(content, "Hello @HackDay")).first()
    println("${sTweet[date].format(HHmm)} | ${sTweet[author][name]}: ${sTweet[content]}")

    val cris = Entity(name eq "@cris", lastLogin eq Instant.now())
    var nTweet: StoredEntity = sTweet.set(content eq "Array set works", likes eq listOf(storedUser, cris))
    nTweet = conn.persist(cris, nTweet).persistedEntities[1]

    println(nTweet[content])
    println(nTweet[likes].map { it[name] })
    val users = nTweet[likes].map { proxy<User>(it) }
    println(users.map { p -> "${p.name()}: ${p.last_login()}" })

    val nUser = users[0].name("@reflection_rulezz")
    conn.persist(nUser.entity())
    println(conn.db.pullAs<User>(nUser.eid()!!)!!.name())

}