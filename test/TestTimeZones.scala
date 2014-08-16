

import helpers.{Converters, ReminderParsing}
import models._
import org.joda.time.DateTime
import org.junit.Test
import org.scalatest.junit.JUnitSuite



class TestTimeZones extends JUnitSuite {

  @Test
  def timeOfdayParsing() {

    lazy val database = helpers.Database.getDatabase()

    val user = database.withSession { implicit session =>
      Users.findById(3).get
    }

    val createdAt = DateTime.now().withZone(ReminderParsing.timeZone)
    val firstTime = DateTime.now().withZone(ReminderParsing.timeZone).plusHours(1)

    // Create A test tweet
    val tweet = database.withSession { implicit session =>
      Tweets.insertAndGet(Tweet(None, 3, 999, "HerbieLewis", Converters.getJsonFromString("{}"), DateTime.now))
    }

    val reminder = Reminder(None, user.id.get, 1, createdAt, Repeat.Never, firstTime, "Foo", tweet.id.get)

    database.withSession { implicit session =>
      Reminders.insert(reminder)
    }
  }
}