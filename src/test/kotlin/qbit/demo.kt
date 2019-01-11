package qbit

import qbit.ns.Namespace
import qbit.schema.RefAttr
import qbit.schema.ScalarAttr
import qbit.schema.eq
import qbit.storage.MemStorage
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val HHmm = DateTimeFormatter.ofPattern("HH:mm")

fun main(args: Array<String>) {
    val tweetNs = Namespace.of("demo", "tweet")
    val userNs = Namespace.of("demo", "user")

    val name = ScalarAttr(userNs["name"], QString, unique = true)
    val lastLogin = ScalarAttr(userNs["last_login"], QInstant)

    val content = ScalarAttr(tweetNs["content"], QString)
    val author = RefAttr(tweetNs["author"])
    val date = ScalarAttr(tweetNs["date"], QZonedDateTime)

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