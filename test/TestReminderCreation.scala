import helpers.ReminderParsing
import models.Repeat
import org.joda.time._
import org.junit.Test
import org.scalatest.junit.JUnitSuite


class TestReminderCreation extends JUnitSuite {


  def testTweetCreation(mention: String, what: String, firstTime: DateTime, repeat: Repeat.Value) {
    val parsed = ReminderParsing.createReminderFromTextAndTime(mention, DateTime.now())
    assert(parsed.isParsedSuccessfully)

    parsed match {
      case ReminderParsing.Success(w, ft, r) =>
        assert(r === repeat)
        assert(w === what)
        assert(ft === firstTime)
      case _ => assert(false)
    }
  }


  def getNextDayOfWeek(dow: Integer): LocalDate = {

    var d = LocalDate.now()

    if (d.getDayOfWeek >= dow) {
      d = d.plusWeeks(1)
    }
    d.withDayOfWeek(dow)
  }


  @Test def createA() {
    val mention = "@remindtweets Remind me to build this app on Tomorrow at 6:00 PM"

    testTweetCreation(mention,
      what="build this app",
      firstTime=DateTime.now().plusDays(1).withTime(18,0,0,0),
      repeat=Repeat.Never)
  }


  @Test def createB() {
    val mention = "@remindtweets Remind me to build this app at 12:01 AM"

    testTweetCreation(mention,
      what="build this app",
      firstTime=DateTime.now().plusDays(1).withZone(DateTimeZone.forID("America/Los_Angeles")).withTime(0,1,0,0),
      repeat=Repeat.Never)
  }


  @Test def createC() {
    val mention = "@remindtweets Remind me to build this app on Wednesday at 6:00PM every week"

    testTweetCreation(mention,
      what="build this app",
      firstTime=getNextDayOfWeek(DateTimeConstants.WEDNESDAY)
        .toDateTimeAtStartOfDay
        .withZone(DateTimeZone.forID("America/Los_Angeles"))
        .withTime(18,0,0,0),
      repeat=Repeat.Weekly)
  }


  @Test def createD() {
    val mention = "@remindtweets Remind me to once again see if this is working tomorrow at 5"

    testTweetCreation(mention,
      what="once again see if this is working tomorrow",
      firstTime=DateTime.now().plusDays(1).withZone(DateTimeZone.forID("America/Los_Angeles")).withTime(5,0,0,0),
      repeat=Repeat.Never)
  }
}