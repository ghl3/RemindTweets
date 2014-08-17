import helpers.ReminderParsing

import org.joda.time.{DateTimeZone, DateTime, LocalTime}
import org.scalatest.junit.JUnitSuite
import org.junit.Test


class TestMentionParsing extends JUnitSuite {

  @Test def timeOfdayParsing() {

    var timeOfday: Option[LocalTime] = None

    timeOfday = ReminderParsing.parseTwelveHour("5:00 PM")
    assert(timeOfday.isDefined)
    assert(timeOfday.get == new LocalTime(17,0,0))

    timeOfday = ReminderParsing.parseTwelveHour("5:00PM")
    assert(timeOfday.isDefined)
    assert(timeOfday.get == new LocalTime(17,0,0))

  }


  @Test def timeParsing() {

    val reminderTime = ReminderParsing.parseReminderTime("5:00 PM")
    assert(reminderTime.isDefined)

    val todayAtFive = DateTime.now().withZone(DateTimeZone.forID("America/Los_Angeles"))
      .withHourOfDay(17)
      .withMinuteOfHour(0)
      .withSecondOfMinute(0).withMillisOfSecond(0)

    assert(todayAtFive === reminderTime.get)

  }


  @Test def structuredReminderA() {
    val mention = "@remindtweets Remind me to build this app on Wednesday at 5:00PM every week"
    val structured = ReminderParsing.parseStatusTextIntoReminderData(mention).get

    assert(structured("what") == "build this app")
    assert(structured("every") == "every week")
    assert(structured("to") == "to")
    assert(structured("at") == "at 5:00PM")
    assert(structured("on") == "on Wednesday")
    assert(structured("time") == "5:00PM")
    assert(structured("repeat") == "week")
    assert(structured("when") == "Wednesday")
  }


  @Test def parsingA() {
    val mention = "@RemindTweets Remind me to eat lunch in 4 hours"
    val parsed = ReminderParsing.parseStatusTextIntoReminderData(mention)
    assert(parsed.isDefined)

    parsed match {
      case Some(data) =>

        assert(!data.contains("repeat"))
        assert(data("what") === "eat lunch")
        assert(data("relativeTime") === "4 hours")

      case _ => assert(false)
    }
  }


  @Test def parsingB() {
    val mention = "@RemindTweets Remind me to get dinner at 5:40pm."
    val parsed = ReminderParsing.parseStatusTextIntoReminderData(mention)
    assert(parsed.isDefined, "Didn't parse successfully")

    parsed match {
      case Some(data) =>

        assert(!data.contains("repeat"))
        assert(data("what") === "get dinner")
        assert(data("time") === "5:40pm")

      case _ => assert(false)
    }
  }



}