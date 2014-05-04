import models.ReminderParsing

import org.joda.time.{LocalDateTime, DateTime, LocalTime}
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

    val todayAtFive = DateTime.now()
      .withHourOfDay(17)
      .withMinuteOfHour(0)
      .withSecondOfMinute(0).withMillisOfSecond(0);

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
    val mention = "@remindtweets Remind me to build this app on Wednesday at 6:00 PM"
    val parsed = ReminderParsing.parseStatusText(mention)
    assert(parsed.isParsedSuccessfully)

    parsed match {
      case ReminderParsing.Success(repeat, firstTime, what) => {
        assert(repeat === null)
        assert(what === "build this app")
        assert(firstTime === DateTime.now().withTime(18,0,0,0))
      }
      case _ => assert(false)
    }

  }

  @Test def parsingB() {
    val mention = "@remindtweets Remind me to build this app at 6:00 PM"
    val parsed = ReminderParsing.parseStatusText(mention)
    assert(parsed.isParsedSuccessfully)
  }

  @Test def parsingC() {
    val mention = "@remindtweets Remind me to build this app on Wednesday at 6:00PM every week"
    val parsed = ReminderParsing.parseStatusText(mention)
    assert(parsed.isParsedSuccessfully)
  }


}