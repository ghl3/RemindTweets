

import helpers.Converters
import models._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTimeZone, LocalDateTime, DateTime}
import org.junit.Test
import org.scalatest.junit.JUnitSuite


//import com.github.tototoshi.slick.converter._

//import org.joda.time._

//import com.github.tminglei.slickpg.


class TestTimeZones extends JUnitSuite {


  @Test
  def testFormatting() = {

    val testTimeZone = DateTimeZone.forID("America/New_York")
    val createdAt = DateTime.now().withZone(testTimeZone)

    val tzDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ")

    val timeString = createdAt.toString(tzDateTimeFormatter)

    println("Time: ", timeString)
    assert(timeString.equals("2014-08-16 19:09:25.155000-0400"))

  }

  @Test
  def timeOfdayParsing() = {

    lazy val database = helpers.Database.getDatabase()

    val user = database.withSession { implicit session =>
      //Database.clearAndCreateFreshUser("TestUser")
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