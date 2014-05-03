

import models.ReminderParsing

import org.scalatest.junit.JUnitSuite
import org.junit.Test

class TestMentionParsing extends JUnitSuite {


  @Test def testCreateEmployeeObjectAndProperties() {

    val mention = "@remindtweets Remind me to build this app on Wednesday at 5:00PM"
    val reminder = ReminderParsing.parseStatusText(mention)

    assert(reminder.isParsedSuccessfully)
  }
}