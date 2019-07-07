# qbit
![Build Status](https://travis-ci.com/d-r-q/qbit.svg?branch=master)

qbit is a ACID [kotlin-]multiplatform embeddable distributed DB with lazy replication and flexible write conflicts resolution toolset. Heavily inspired by [Datomic](https://www.datomic.com/)

## Vision

qbit - it's storage and replication technology. qbit implements:
 1) Automatic synchronization of user data with cloud, if cloud is presented in the system;
 2) Automatic synchronization of user data between devices, when direct connection between devices is available;
 3) Automatic write conflicts resolution with sane defaults and rich custom policies;
 4) CRDT on DB level;
 5) Encryption of user data with user provided password, so it's is protected from access by third-parties including cloud provider.
 
qbit stores data locally and uses entity graph information model, so for developers it's means that there are no usual persistence points of pain:
 1) There are no more monstrous queries for round-trips minimization, since there is no more round-trips;
 2) There are no more object-relational mappers, since there is no more object-relational mismatch.
 
## Mission

Make internet decentralized again. And make development fun again.

## Roadmap
 * Datastore
   * :white_check_mark: ~Fetch entity by id~
   * :white_check_mark: ~FileSystem storage~
   * :white_check_mark: ~Reference attributes~
   * :white_check_mark: ~Multivalue attributes~
   * Component attributes
   
 * DataBase
   * :white_check_mark: ~Query by attribute value~
   * :white_check_mark: ~Schema~
   * :white_check_mark: ~Unique constraints~
   * :white_check_mark: ~Programmatic range queries~
   * :white_check_mark: ~Pull entites via reference attributes~
   * :white_check_mark: ~Typed entities~
   * :white_check_mark: ~Local ACID transactions~
   * Programmatic joins
   * Query language (Datalog and/or something SQL-like)
   
 * p2p DataBase
   * Concurrent writes support
   * Automatic conflict resolution
   * CRDTs
   * p2p data synchronization
   
 * Cloud platform
   * qbit DBMS
   
## Platforms

 * Supported
   * JVM >= 8
   * Android >= 21
 
 * Planned
   * JS
   * Kotlin/Native
   * iOS
   
## Sample code

```kotlin

// schema
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

// Typed wrapper
class User<E : EID?>(entity: Entity<E>) : TypedEntity<E>(entity) {

    var name: String by AttrDelegate(Users.name)

    val lastLogin: Instant by AttrDelegate(Users.lastLogin)

}

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
```
