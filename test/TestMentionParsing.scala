import helpers.ReminderParsing
import models.Repeat

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
    val structured = ReminderParsing.getStructuredReminderResult(mention)

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
    val mention = "@remindtweets Remind me to build this app on Tomorrow at 6:00 PM"
    val parsed = ReminderParsing.parseStatusText(mention)
    assert(parsed.isParsedSuccessfully)

    parsed match {
      case ReminderParsing.Success(what, firstTime, repeat) =>
        assert(repeat === Repeat.Never)
        assert(what === "build this app")
        assert(firstTime === DateTime.now().plusDays(1).withTime(18,0,0,0))
      case _ => assert(false)
    }
  }

  @Test def parsingB() {
    val mention = "@remindtweets Remind me to build this app at 12:01 AM"
    val parsed = ReminderParsing.parseStatusText(mention)
    assert(parsed.isParsedSuccessfully)

    parsed match {
      case ReminderParsing.Success(what, firstTime, repeat) =>
        assert(repeat === Repeat.Never)
        assert(what === "build this app")
        assert(firstTime === DateTime.now().plusDays(1).withZone(DateTimeZone.forID("America/Los_Angeles")).withTime(0,1,0,0))
      case _ => assert(false)
    }
  }

  @Test def parsingC() {
    val mention = "@remindtweets Remind me to build this app on Wednesday at 6:00PM every week"
    val parsed = ReminderParsing.parseStatusText(mention)
    assert(parsed.isParsedSuccessfully, "Should be parsed successfully")

    parsed match {
      case ReminderParsing.Success(what, firstTime, repeat) =>
        assert(repeat === Repeat.Weekly)
        assert(what === "build this app")

        var assertTime = DateTime.now().withDayOfWeek(3).withZone(DateTimeZone.forID("America/Los_Angeles")).withTime(18,0,0,0)
        if (assertTime.isBefore(DateTime.now())) {
          assertTime = assertTime.plusWeeks(1)
        }
        assert(firstTime === assertTime)
      case _ => assert(false, "Not parsed successfully")
    }
  }


  @Test def parsingD() {
    val mention = "@remindtweets Remind me to once again see if this is working tomorrow at 5"
    val parsed = ReminderParsing.parseStatusText(mention)
    assert(parsed.isParsedSuccessfully)

    parsed match {
      case ReminderParsing.Success(what, firstTime, repeat) =>
        assert(repeat === Repeat.Never)
        assert(what === "once again see if this is working tomorrow")
        assert(firstTime === DateTime.now().plusDays(1).withZone(DateTimeZone.forID("America/Los_Angeles")).withTime(5,0,0,0))
      case _ => assert(false)
    }
  }


}