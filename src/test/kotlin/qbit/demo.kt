package qbit

import qbit.ns.Namespace
import qbit.schema.*
import qbit.storage.MemStorage
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val HHmm = DateTimeFormatter.ofPattern("HH:mm")

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
    val storedUser = conn.persist(tweet).createdEntities[user]!!
    conn.persist(storedUser.set(lastLogin, Instant.now()))

    val sTweet = conn.db.query(attrIs(content, "Hello @HackDay")).first()
    println("${sTweet[date].format(HHmm)} | ${sTweet[author][name]}: ${sTweet[content]}")

    val cris = Entity(name eq "@cris", lastLogin eq Instant.now())
    var nTweet: StoredEntity = sTweet.set(content eq "Array set works", likes eq listOf(storedUser, cris))
    nTweet = conn.persist(cris, nTweet).persistedEntities[1]

    println(nTweet[content])
    println(nTweet[likes].map { it[name] })
}