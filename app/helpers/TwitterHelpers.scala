package helpers

import play.Logger

import models.{Reminders, Users}
import models.Tweets.TweetHelpers
import TwitterApi.Status
import play.api.db.slick.Session
import play.api.libs.json.JsValue


object TwitterHelpers {

  def handleMention(mention: Status, json: JsValue)(implicit s: Session) = {

    // Check if it's an existing user
    val user = Users.getOrCreateUser(mention.getUser.getScreenName)

    Logger.info("Handling mention: {} for user {}", mention, user)

    val tweet = TweetHelpers.fromStatusAndJson(user.get, mention, json)
    val parsed = ReminderParsing.parseStatusText(mention.getText)

    Reminders.createAndSaveIfReminder(user.get, tweet, parsed)

  }
}
