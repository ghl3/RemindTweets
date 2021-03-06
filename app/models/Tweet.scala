package models

import org.joda.time.DateTime

import app.MyPostgresDriver.simple._
import helpers.Converters

import play.api.libs.json.JsValue

import helpers.TwitterApi.Status


case class Tweet(id: Option[Long], userId: Long, twitterId: Long, screenName: String, content: JsValue, fetchedAt: DateTime) {

  // We internally store a twitter4j object
  // for convenience
  val obj = Converters.createStatusFromJson(content).getOrElse(null)

  // Convert the internal json4s object to a string
  def jsonString(): String = {
    content.toString()
  }

  // Convert the internal json4s object to a
  // twitter4j Status object
  def getStatus: Status = {
    obj
  }

}


class Tweets(tag: Tag) extends Table[Tweet](tag, "tweets") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc) // This is the primary key column
  def userId = column[Long]("user_id")
  def twitterId = column[Long]("twitter_id", O.NotNull)
  def screenName = column[String]("screen_name", O.NotNull)
  def content = column[JsValue]("content")
  def fetchedAt = column[DateTime]("fetchedat")

  def * = (id.?, userId, twitterId, screenName, content, fetchedAt) <> (Tweet.tupled, Tweet.unapply _)

  def uniqueTwitterId = index("UNIQUE_TWEET_TWITTERID", twitterId, unique = true)

  // Create a foreign key relationship on users
  def user = foreignKey("TWEET_USER_FK", userId, Users.users)(_.id)
}


object Tweets {

  val tweets = TableQuery[Tweets]

  def findById(id: Long)(implicit s: Session): Option[Tweet] = {
    tweets.where(_.id === id).firstOption
  }

  def findByTwitterId(twitterId: Long)(implicit s: Session): Option[Tweet] = {
    tweets.where(_.twitterId === twitterId).firstOption
  }

  def findByUserId(userId: Long)(implicit s: Session) = {
    tweets.where(_.userId === userId).list
  }

  def insert(tweet: Tweet)(implicit s: Session) {
    tweets.insert(tweet)
  }

  def insertAndGet(tweet: Tweet)(implicit s: Session): Tweet = {
    val userId = (tweets returning tweets.map(_.id)) += tweet
    tweet.copy(id = Some(userId))
  }

  def insertIfUniqueTweetAndGet(tweet: Tweet)(implicit s: Session): Tweet = {
    val existingTweet = Tweets.findByTwitterId(tweet.twitterId)
    if (existingTweet.isDefined) {
      existingTweet.get
    } else {
      Tweets.insertAndGet(tweet)
    }
  }

  def update(id: Long, tweet: Tweet)(implicit s: Session) {
    val tweetToUpdate: Tweet = tweet.copy(Some(id))
    tweets.where(_.id === id).update(tweetToUpdate)
  }

  def delete(id: Long)(implicit s: Session) {
    tweets.where(_.id === id).delete
  }

  def fromStatusAndJson(user: User, status: Status, json: JsValue): Tweet = {
    Tweet(None, user.id.get, status.getId, status.getUser.getScreenName, json, DateTime.now())
  }
}