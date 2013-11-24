package models

import org.joda.time.LocalDateTime

import app.MyPostgresDriver.simple._
import org.json4s.JValue
import org.json4s.native.Serialization.write
import org.json4s.native.JsonMethods

import scala.slick.lifted._

import play.api.Play.current

import play.Logger


// SEE: https://github.com/ThomasAlexandre/slickcrudsample/
// http://java.dzone.com/articles/getting-started-play-21-scala

// TODO: Convert to DateTime
case class Tweet(id: Option[Long], twitterId: Long, screenName: String, content: JValue, fetchedAt: LocalDateTime) {

  // Convert the internal json4s object to a string
  def jsonString(): String = {
    implicit val formats = org.json4s.DefaultFormats
    return  write(content) //content.extract[String]
  }

  // Convert the internal json4s object to a
  // twitter4j Status object
  def getStatus: twitter4j.Status = {
    try {
      val testString: String = "{createdAt=2013-11-20, id=404331790371807232, text='@remindtweets This looks interesting', source='web', isTruncated=false, inReplyToStatusId=-1, inReplyToUserId=2205796142, isFavorited=false, inReplyToScreenName='remindtweets', geoLocation=null, place=null, retweetCount=0, isPossiblySensitive=false, contributorsIDs=J@a49c723, retweetedStatus=null, userMentionEntities=[{name='George', screenName='remindtweets', id=2205796142}], urlEntities=[], hashtagEntities=[], mediaEntities=[], currentUserRetweetId=-1, user={id=42805000, name='Herbie Lewis', screenName='HerbieLewis', location='New York', description='Ending the world one proton at a time...', isContributorsEnabled=false, profileImageUrl='http://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', profileImageUrlHttps='https://pbs.twimg.com/profile_images/1333372815/MeBeer_normal.jpg', url='http://t.co/Wp8W6tZCbf', isProtected=false, followersCount=123, status=null, profileBackgroundColor='C0DEED', profileTextColor='333333', profileLinkColor='0084B4', profileSidebarFillColor='DDEEF6', profileSidebarBorderColor='C0DEED', profileUseBackgroundImage=true, showAllInlineMedia=false, friendsCount=436, createdAt=2011-11-23, favouritesCount=12, utcOffset=-21600, timeZone='Central Time (US & Canada)', profileBackgroundImageUrl='http://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundImageUrlHttps='https://abs.twimg.com/images/themes/theme1/bg.png', profileBackgroundTiled=false, lang='en', statusesCount=1530, isGeoEnabled=false, isVerified=false, translator=false, listedCount=2, isFollowRequestSent=false}}"
      return twitter4j.json.DataObjectFactory.createStatus(testString)
    }
    catch {
      case e: Exception =>
        Logger.error("Failed to create twitter status", e)
        return null
    }
    return null
  }

}


object Tweet {

  def fromStatus(status: twitter4j.Status): Tweet = {
    val now = LocalDateTime.now()
    val statusJson: String = twitter4j.json.DataObjectFactory.getRawJSON(status)

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
