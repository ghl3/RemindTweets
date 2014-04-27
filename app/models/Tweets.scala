package models

import org.joda.time.LocalDateTime

import app.MyPostgresDriver.simple._
//import org.json4s.native.Serialization.write
//import org.json4s.native.JsonMethods

//import org.json4s._
//import scala.slick.lifted._

//import play.Logger
import helpers.Converters
//import org.json4s.JValue

import play.api.libs.json.JsValue
//import play.api.libs.json.Json


//import app.MyPostgresDriver.simple.Tag


//import app.WithMyDriver

//import play.api.libs.json.JsValue

//import helpers.Database.getDatabase

// SEE: https://github.com/ThomasAlexandre/slickcrudsample/
// http://java.dzone.com/articles/getting-started-play-21-scala

// TODO: Convert to DateTime
// TODO: Remove the JValue as a member.  Should only take a Status, store that, and convet it to JValue for database persistence
case class Tweet(id: Option[Long], twitterId: Long, screenName: String, content: JsValue, fetchedAt: LocalDateTime) {

  // We internally store a twitter4j object
  // for convenience
  val obj = Converters.createStatusFromJson(content)

  // Convert the internal json4s object to a string
  def jsonString(): String = {
    content.toString();
    //implicit val formats = org.json4s.DefaultFormats
    //return write(content) //content.extract[String]
  }

  // Convert the internal json4s object to a
  // twitter4j Status object
  def getStatus: twitter4j.Status = {
    return obj
  }

}

/*
object Tweet {

  def fromStatus(status: twitter4j.Status): Tweet = {
    val now = LocalDateTime.now()
    val statusJson: String = Converters.getJsonStringFromStatus(status)

    Logger.info("Creating Tweet from status: {} json: {}", status, statusJson)
    val json: JsValue = Json.parse(statusJson); //JsonMethods.parse(statusJson)

    return Tweet(None, status.getId, status.getUser.getScreenName, json, now)
  }


}
*/

// Definition of the COFFEES table
class Tweets(tag: Tag) extends Table[Tweet](tag, "tweets") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc) // This is the primary key column
  def twitterId = column[Long]("twitterid", O.NotNull)
  def screenName = column[String]("screenName", O.NotNull)
  def content = column[JsValue]("content")
  def fetchedAt = column[LocalDateTime]("fetchedat")

  def * = (id.?, twitterId, screenName, content, fetchedAt) <> (Tweet.tupled, Tweet.unapply _)
}


object Tweets {

  val tweets = TableQuery[Tweets]


}


/*
object Tweets{

  def addToTable(tweet: Tweet): Tweet = {
    getDatabase().withSession { implicit session: Session =>
      val id = Tweets.autoInc.insert(tweet.twitterId, tweet.screenName, tweet.content, tweet.fetchedAt)
      return fetch(id).get
    }
  }

  def update(tweet: Tweet): Tweet = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      val id = Tweets.insert(tweet)
      return fetch(id).get
    }
  }

  def fetch(id: Long): Option[Tweet] = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
      (for { b <- Tweets if b.id is id} yield b).firstOption
    }
  }

}
*/