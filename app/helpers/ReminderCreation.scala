package helpers

import models._
import play.Logger


object ReminderCreation {

  def handleStatus(status: twitter4j.Status): Unit = {

    /*
    if (Reminder.isReminder(status)) {
      if (User.isNewUser(status.getUser)) {
        var user = User.createUser(status.getUser)
        user = Users.addToTable(user)
        Logger.info("Added user: {}", user.screenName)
      }
      val reminder = Reminder.createReminder(status)
      if (reminder.isEmpty) {
        Logger.error("Failed to create reminder from status: {}", status)
      } else {
        Reminders.addToTable(reminder.get)
      }
    }
    else {
    }

    var tweet = Tweet.fromStatus(status)
    tweet = Tweets.addToTable(tweet)
    Logger.info("Tweet: {} {}", tweet.id, tweet.jsonString)
  }
*/
  }

}
