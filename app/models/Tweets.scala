package models

import org.joda.time.LocalDateTime

import app.MyPostgresDriver.simple._
import org.json4s.JValue
import org.json4s.native.Serialization.write
import org.json4s.native.JsonMethods

import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.slick.lifted._

import play.api.Play.current

import play.Logger
import helpers.TwitterApi
import helpers.Converters



// SEE: https://github.com/ThomasAlexandre/slickcrudsample/
// http://java.dzone.com/articles/getting-started-play-21-scala

// TODO: Convert to DateTime
// TODO: Remove the JValue as a member.  Should only take a Status, store that, and convet it to JValue for database persistence
case class Tweet(id: Option[Long], twitterId: Long, screenName: String, content: JValue, fetchedAt: LocalDateTime) {

  // We internally store a twitter4j object
  // for convenience
  val obj = Converters.createStatusFromJson(content)

  // Convert the internal json4s object to a string
  def jsonString(): String = {
    implicit val formats = org.json4s.DefaultFormats
    return  write(content) //content.extract[String]
  }

  // Convert the internal json4s object to a
  // twitter4j Status object
  def getStatus: twitter4j.Status = {
    return obj
  }
  /*
    try {
      return twitter4j.json.DataObjectFactory.createStatus(TwitterApi.dummyJsonB)
    }
    catch {
      case e: Exception =>
        Logger.error("Failed to create twitter status", e)
        return null
    }
    return null
  }
  */

}


object Tweet {

  def fromStatus(status: twitter4j.Status): Tweet = {
    val now = LocalDateTime.now()
    val statusJson: String = Converters.getJsonStringFromStatus(status)

    Logger.info("Creating Tweet from status: {} json: {}", status, statusJson)
    val json: org.json4s.JValue = JsonMethods.parse(statusJson)

    return Tweet(None, status.getId, status.getUser.getScreenName, json, now)
  }

}


// Definition of the COFFEES table
object Tweets extends Table[Tweet]("tweets") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc) // This is the primary key column
  def twitterId = column[Long]("twitterid", O.NotNull)
  def screenName = column[String]("screenName", O.NotNull)
  def content = column[JValue]("content")
  def fetchedAt = column[LocalDateTime]("fetchedat")

  def * : ColumnBase[Tweet] = (id.? ~ twitterId ~ screenName ~ content ~ fetchedAt) <> (Tweet .apply _, Tweet.unapply _)

  // These are both necessary for auto increment to work with psql
  def autoInc = twitterId ~ screenName ~ content ~ fetchedAt returning id


  def addToTable(tweet: Tweet): Tweet = {
    play.api.db.slick.DB.withSession{implicit session: Session =>
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
