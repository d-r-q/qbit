package qbit.test.twitter

import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Long?, val name: String)

@Serializable
data class Tweet(val id: Long?, val user: User, val text: String)

@Serializable
data class Like(val id: Long?, val user: User, val tweet: Tweet)
