
import helpers.ReminderParsing
import helpers.ReminderParsing.DateTooEarly
import models.Repeat
import org.joda.time._
import org.junit.Test
import org.scalatest.junit.JUnitSuite


class TestReminderCreation extends JUnitSuite {


  def testTweetCreation(mention: String, what: String, firstTime: DateTime, repeat: Repeat.Value) {
    val parsed = ReminderParsing.createReminderFromTextAndTime(mention, DateTime.now())

    parsed match {
      case DateTooEarly(time) => println("Time: %s".format(time))
      case  _ => Unit
    }

    assert(parsed !== DateTooEarly, "Date should not be before right now")

    assert(parsed.isParsedSuccessfully, "Should be parsed successfully")

    parsed match {
      case ReminderParsing.Success(w, ft, r) =>
        assert(r === repeat, "(%s) should equal (%s)".format(r, repeat))
        assert(w === what, "(%s) should equal (%s)".format(w, what))
        assert(ft.withMillisOfSecond(0) === firstTime.withMillisOfSecond(0))
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


  @Test def createE() {
    val mention = "@remindtweets Remind me to get dessert in 4 hours and 15 minutes"

    testTweetCreation(mention,
      what="get dessert",
      firstTime=DateTime.now().plusHours(4).plusMinutes(15).withZone(DateTimeZone.forID("America/Los_Angeles")),
      repeat=Repeat.Never)
  }


  @Test def createF() {
    val mention = "@remindtweets Remind me to get dessert in 2 days and 50 hours and 30 seconds"

    testTweetCreation(mention,
      what="get dessert",
      firstTime=DateTime.now().plusDays(2).plusHours(50).plusSeconds(30).withZone(DateTimeZone.forID("America/Los_Angeles")),
      repeat=Repeat.Never)
  }


  @Test def createG() {
    val mention = "@remindtweets Remind  me  to  get  dessert  in  three  hours  and  fifteen  minutes  and  twelve  seconds"

    testTweetCreation(mention,
      what="get  dessert",
      firstTime=DateTime.now().plusHours(3).plusMinutes(15).plusSeconds(12).withZone(DateTimeZone.forID("America/Los_Angeles")),
      repeat=Repeat.Never)
  }


  @Test def createH() {

    val mention = "@remindtweets Remind me to go to sleep every day at midnight."

    testTweetCreation(mention,
      what="go to sleep",
      firstTime=DateTime.now().plusDays(1).withTimeAtStartOfDay.withZone(DateTimeZone.forID("America/Los_Angeles")),
      repeat=Repeat.Daily)
  }


  @Test def createI() {

    val mention = "@remindtweets Remind me to eat my lunch every Tuesday at noon."

    testTweetCreation(mention,
      what="eat my lunch",
      firstTime=getNextDayOfWeek(DateTimeConstants.TUESDAY)
        .toDateTimeAtStartOfDay
        .withHourOfDay(12)
        .withZone(DateTimeZone.forID("America/Los_Angeles")),
      repeat=Repeat.Weekly)
  }

}