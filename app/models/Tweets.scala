package models

import org.joda.time.DateTime

import app.MyPostgresDriver.simple._
import play.Logger
import helpers.Converters

import play.api.libs.json.JsValue
import play.api.libs.json.Json


// SEE: https://github.com/ThomasAlexandre/slickcrudsample/
// http://java.dzone.com/articles/getting-started-play-21-scala

// TODO: Convert to DateTime
// TODO: Remove the JValue as a member.  Should only take a Status, store that, and convet it to JValue for database persistence
case class Tweet(id: Option[Long], userId: Long, twitterId: Long, screenName: String, content: JsValue, fetchedAt: DateTime) {

  // We internally store a twitter4j object
  // for convenience
  val obj = Converters.createStatusFromJson(content)

  // Convert the internal json4s object to a string
  def jsonString(): String = {
    content.toString()
  }

  // Convert the internal json4s object to a
  // twitter4j Status object
  def getStatus: twitter4j.Status = {
    obj
  }

}


class Tweets(tag: Tag) extends Table[Tweet](tag, "tweets") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc) // This is the primary key column
  def userId = column[Long]("userId")
  def twitterId = column[Long]("twitterid", O.NotNull)
  def screenName = column[String]("screenName", O.NotNull)
  def content = column[JsValue]("content")
  def fetchedAt = column[DateTime]("fetchedat")

  def * = (id.?, userId, twitterId, screenName, content, fetchedAt) <> (Tweet.tupled, Tweet.unapply _)
}


object Tweets {

  val tweets = TableQuery[Tweets]

  def findById(id: Long)(implicit s: Session): Option[Tweet] = {
    tweets.where(_.id === id).firstOption
  }

  def insert(tweet: Tweet)(implicit s: Session) {
    tweets.insert(tweet)
  }

  def insertAndGet(tweet: Tweet)(implicit s: Session): Tweet = {
    val userId = (tweets returning tweets.map(_.id)) += tweet
    return tweet.copy(id = Some(userId))
  }

  def update(id: Long, tweet: Tweet)(implicit s: Session) {
    val tweetToUpdate: Tweet = tweet.copy(Some(id))
    tweets.where(_.id === id).update(tweetToUpdate)
  }

  def delete(id: Long)(implicit s: Session) {
    tweets.where(_.id === id).delete
  }


  object TweetHelpers {

    def fromStatus(user: User, status: twitter4j.Status): Tweet = {
      val statusJson: String = Converters.getJsonStringFromStatus(status)

      Logger.info("Creating Tweet from status: {} json: {}", status, statusJson)
      val json: JsValue = Json.parse(statusJson); //JsonMethods.parse(statusJson)

      return Tweet(None, user.id.get, status.getId, status.getUser.getScreenName, json, DateTime.now())
    }
  }

  def getUserTweetsFromTimeline(user: User, timeline: Iterable[twitter4j.Status]) : Iterable[Tweet] = {
    for (status <- timeline) yield TweetHelpers.fromStatus(user, status)
  }

}