import models.ReminderParsing

import org.joda.time.LocalTime
import org.scalatest.junit.JUnitSuite
import org.junit.Test


class TestMentionParsing extends JUnitSuite {

  @Test def timeParsing() {

    val timeOfday = ReminderParsing.parseTwelveHour("5:00 PM")
    assert(timeOfday.isDefined)
    assert(timeOfday.get == new LocalTime(17,0,0))
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
    val mention = "@remindtweets Remind me to build this app on Wednesday at 5:00PM"
    val reminder = ReminderParsing.parseStatusText(mention)
    assert(reminder.isParsedSuccessfully)
  }

  @Test def parsingB() {
    val mention = "@remindtweets Remind me to build this app at 5:00PM"
    val reminder = ReminderParsing.parseStatusText(mention)
    assert(reminder.isParsedSuccessfully)
  }

  @Test def parsingC() {
    val mention = "@remindtweets Remind me to build this app on Wednesday at 5:00PM every week"
    val reminder = ReminderParsing.parseStatusText(mention)
    assert(reminder.isParsedSuccessfully)
  }


}