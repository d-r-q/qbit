package qbit

import qbit.ns.Namespace
import qbit.schema.RefAttr
import qbit.schema.ScalarAttr
import qbit.storage.MemStorage
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


fun main(args: Array<String>) {
    val ns = Namespace("demo")

    val name = ScalarAttr(ns.subNs("user")["name"], QString, unique = true)

    val content = ScalarAttr(ns.subNs("tweet")["content"], QString)
    val author = RefAttr(ns.subNs("tweet")["author"])
    val date = ScalarAttr(ns.subNs("tweet")["date"], QZonedDateTime)

    val conn = qbit(MemStorage())
    conn.persist(name, content, author, date)

    val user = Entity(name eq "@azhidkov")
    val tweet = Entity(content eq "Hello @HackDay",
            author eq user,
            date eq ZonedDateTime.now())
    conn.persist(tweet)

    val sTweet = conn.db.query(attrIs(content, "Hello @HackDay")).first()
    println("${sTweet[date]!!.format(DateTimeFormatter.ofPattern("HH:mm"))} | ${sTweet[author]!![name]}: ${sTweet[content]}")
}