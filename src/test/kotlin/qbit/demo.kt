package qbit

import qbit.model.*
import qbit.ns.Namespace
import qbit.storage.MemStorage
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val HHmm = DateTimeFormatter.ofPattern("HH:mm")

fun main(args: Array<String>) {
    val tweetNs = Namespace.of("demo", "tweet")
    val userNs = Namespace.of("demo", "user")

    val name = ScalarAttr(userNs["name"], DataType.QString, unique = true)
    val lastLogin = ScalarAttr(userNs["last_login"], DataType.QInstant)

    val content = ScalarAttr(tweetNs["content"], DataType.QString)
    val author = RefAttr(tweetNs["author"])
    val date = ScalarAttr(tweetNs["date"], DataType.QZonedDateTime)

    val conn = qbit(MemStorage())
    conn.persist(name, lastLogin, content, author, date)

    val user = Entity(name eq "@azhidkov", lastLogin eq Instant.now())
    val tweet = Entity(content eq "Hello @HackDay",
            author eq user,
            date eq ZonedDateTime.now())
    val storedUser = conn.persist(tweet).createdEntities[user]!!
    conn.persist(storedUser.set(lastLogin, Instant.now()))

    val sTweet = conn.db.query(attrIs(content, "Hello @HackDay")).first()
    println("${sTweet[date]!!.format(HHmm)} | ${sTweet[author]!![name]}: ${sTweet[content]}")
}