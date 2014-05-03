

import models.ReminderParsing

import org.scalatest.junit.JUnitSuite
import org.junit.Test

class TestMentionParsing extends JUnitSuite {


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


  @Test def structuredReminderA() {

    val mention = "@remindtweets Remind me to build this app on Wednesday at 5:00PM every week"

    val structured = ReminderParsing.getStructuredReminderResult(mention)

    println("Thing: " + structured)

  }

}