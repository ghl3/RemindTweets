

import helpers.{Database, Converters, ReminderParsing}
import models._
import org.joda.time.{LocalDateTime, DateTime}
import org.junit.Test
import org.scalatest.junit.JUnitSuite



class TestTimeZones extends JUnitSuite {

  @Test
  def timeOfdayParsing() {

    lazy val database = helpers.Database.getDatabase()

    val user = database.withSession { implicit session =>
      //Database.clearAndCreateFreshUser("TestUser")
      Users.clearUserIfExists("TestUser")
      Users.insertAndGet(User(None, "TestUser", LocalDateTime.now()))
    }

    val twitterId = scala.util.Random.nextInt()

    // Create a tests tweet to attach the reminder to
    val tweet = database.withSession { implicit session =>
      Tweets.insertAndGet(Tweet(None, user.id.get, twitterId, user.screenName, Converters.getJsonFromString("{}"), DateTime.now))
    }

    val createdAt = DateTime.now().withZone(ReminderParsing.timeZone)
    val firstTime = DateTime.now().withZone(ReminderParsing.timeZone).plusHours(1)

    val reminder = Reminder(None, user.id.get, twitterId, createdAt, Repeat.Never, firstTime, "Foo", tweet.id.get)

    database.withSession { implicit session =>
      Reminders.insert(reminder)
    }
  }
}