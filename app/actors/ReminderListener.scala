package actors

import akka.actor._
import play.Logger
import helpers.{ReminderParsing, TwitterApi}

import helpers.TwitterApi.{TweetListener, Paging, Status, TwitterStatusAndJson}

import play.api.Play.current

import play.api.libs.json.JsValue
import models.{Tweets, Users, Reminders}
import play.api.db.slick.Session


object ReminderListener {


  def beginListeningStreaming() = {

    // Create an Akka system
    val system = ActorSystem("MentionListener")

    // Create a set of parser actors
    val parsers = system.actorOf(Props(new ReminderParser), name="masterParsers")

    //twitterStream
    val twitterStream = TwitterApi.getTwitterStream

    // Create a listener that parses any incoming tweet
    twitterStream.addListener(new TweetListener((status: Status) => {
      // Have to get the json here in the same thread
      val mention = new TwitterStatusAndJson(status)
      parsers ! ParseAndHandleMention(mention.status, mention.json)
    }))

    // Setup a search for our handle
    val search = new twitter4j.FilterQuery().track(Array("@remindtweets"))
    twitterStream.filter(search)
  }


  def fetchAndParseLatestTweets() = {
    for (parsed <- getLatestMentions) {
      play.api.db.slick.DB.withSession { implicit session =>
        ReminderParser.handleMention(parsed.mention, parsed.json)
      }
    }
  }


  // Used when we want to catch up since downtime
  def getLatestMentions: Seq[ParseAndHandleMention] = {

    val maxMentionId: Option[Long] = play.api.db.slick.DB.withSession { implicit session =>
      Reminders.getLatestReminderTwitterId()
    }

    val paging: Paging = new Paging()
    paging.setCount(TwitterApi.MAX_TIMELINE_TWEETS)
    if (maxMentionId.isDefined) {
      Logger.info("Getting mentions since twitter id: %s".format(maxMentionId.get))
      paging.setSinceId(maxMentionId.get)
    }

    val mentions = TwitterApi.getMentionsAndJsonTimeline(paging)
    for (mention: TwitterStatusAndJson <- mentions) yield ParseAndHandleMention(mention.status, mention.json)
  }


}


case class ParseAndHandleMention(mention: Status, json: JsValue)

class ReminderParser extends Actor {

  override def receive: Receive = {

    case ParseAndHandleMention(mention, json) =>

      Logger.info("Actor Handling mention: {} {}", mention, json)

      play.api.db.slick.DB.withSession { implicit session =>
          ReminderParser.handleMention(mention, json)
      }
  }
}


object ReminderParser {

  def handleMention(mention: Status, json: JsValue)(implicit s: Session) = {

    // Check if it's an existing user
    val user = Users.getOrCreateUser(mention.getUser.getScreenName)

    Logger.info("Handling mention: {} for user {}", mention, user)

    val tweet = Tweets.fromStatusAndJson(user.get, mention, json)

    try {
      val parsed = ReminderParsing.parseStatusText(mention.getText)
      Reminders.createAndSaveIfReminder(user.get, tweet, parsed)
    } catch {
      case e: Exception =>
        Logger.error("Failed to parse mention {} and json {} from user {}", mention, json, user)
    }
  }
}