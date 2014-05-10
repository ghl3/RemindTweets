package helpers

import play.Logger

import scala.collection.JavaConverters._
import models.{Reminders, ReminderParsing, Users}
import models.Tweets.TweetHelpers
import twitter4j.Status
import play.api.db.slick.Session


object TwitterHelpers {

  def handleMention(mention: Status)(implicit s: Session) = {

    // Check if it's an existing user
    val user = Users.getOrCreateUser(mention.getUser.getScreenName)

    Logger.info("Handling mention: {} for user {}", mention, user)

    val tweet = TweetHelpers.fromStatus(user.get, mention)
    val parsed = ReminderParsing.parseStatusText(mention.getText)

    Reminders.createAndSaveIfReminder(user.get, tweet, parsed)

  }


  /*
  def getMentions = {
    Logger.info("Getting status for {} {}", TwitterApi.getTwitter.getScreenName, TwitterApi.getTwitter.getId: java.lang.Long)

    val mentions = TwitterApi.getMentions.asScala.iterator
    Logger.info("Mentions: %s".format(mentions))

    for (mention <- mentions) {

      val jsonString = Converters.getJsonStringFromStatus(mention)
      Logger.info("Got JSON String: {}", jsonString)

      val json = Converters.getJsonFromStatus(mention)
      Logger.info("Got JSON: {}", json)

      // Check if it's an existing user
      val user = Users.getOrCreateUser(mention.getUser.getScreenName)

      Logger.info("Handling mention: {} for user {}", mention, user)

      val tweet = TweetHelpers.fromStatus(user.get, mention)
      val parsed = ReminderParsing.parseStatusText(mention.getText)

      Reminders.createAndSaveIfReminder(user.get, tweet, parsed)
    }
  }
  */
}
