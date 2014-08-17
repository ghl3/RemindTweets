
import helpers.Converters
import models._
import org.joda.time.{DateTimeZone, LocalDateTime, DateTime}
import org.junit.Test
import org.scalatest.junit.JUnitSuite


class TestTimeZones extends JUnitSuite {


  @Test
  def timeOfdayParsing() = {

    lazy val database = helpers.Database.getDatabase()

    val user = database.withSession { implicit session =>
      Users.clearUserIfExists("TestUser")
      Users.insertAndGet(User(None, "TestUser", LocalDateTime.now()))
    }

    val twitterId = scala.util.Random.nextInt()

    // Create a tests tweet to attach the reminder to
    val tweet = database.withSession { implicit session =>
      Tweets.insertAndGet(Tweet(None, user.id.get, twitterId, user.screenName, Converters.getJsonFromString(TestData.dummyJsonALong), DateTime.now))
    }

    val testTimeZone = DateTimeZone.forID("America/New_York")

    val createdAt = DateTime.now().withZone(testTimeZone)
    val firstTime = DateTime.now().withZone(testTimeZone).plusHours(1)

    assert(createdAt.getZone.equals(testTimeZone))

    val reminder = database.withSession { implicit session =>
      Reminders.insertAndGet(Reminder(None, user.id.get, twitterId, createdAt, Repeat.Never, firstTime, "Foo", tweet.id.get))
    }

    assert(reminder.id.isDefined)

  }
}