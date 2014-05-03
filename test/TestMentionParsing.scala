

import models.{ReminderParsing, ReminderHelper}

import org.scalatest.junit.JUnitSuite
import org.junit.Test

import play.Logger

class TestMentionParsing extends JUnitSuite {


  @Test def testCreateEmployeeObjectAndProperties() {


    val mention = "@remindtweets Remind me to build this app on Wednesday at 5:00PM";

    val reminder = ReminderHelper.parseStatusText(mention)

    reminder match {
      case ReminderParsing.Success(_,_,_) => Logger.info("Successfully parsed reminder: {}", reminder)
      case _ => assert(false)
    }
  }

}