import helpers.ReminderParsing
import models.Repeat

import org.joda.time.{DateTimeZone, DateTime, LocalTime}
import org.scalatest.junit.JUnitSuite
import org.junit.Test


class TestMentionParsing extends JUnitSuite {


  def assertParsedProperly(mention: String, expectedValues: Map[String,String], notPresentKeys: List[String]): Unit = {

    val parsed = ReminderParsing.parseStatusTextIntoReminderData(mention)
    assert(parsed.isDefined)

    parsed match {
      case Some(data) =>
        for ((key, value) <- expectedValues) {
          assert(data.contains(key) === true)
          assert(data(key) === value)
        }
        for (key <- notPresentKeys) {
          assert(data.contains(key) === false)
        }

      case _ => assert(false, "Reminder not parsed")
    }
  }

  def assertParsedProperly(mention: String, expectedValues: Map[String,String]): Unit = assertParsedProperly(mention, expectedValues, List())


  @Test
  def timeOfdayParsing() {

    var timeOfday: Option[LocalTime] = None

    timeOfday = ReminderParsing.parseTwelveHour("5:00 PM")
    assert(timeOfday.isDefined)
    assert(timeOfday.get == new LocalTime(17,0,0))

    timeOfday = ReminderParsing.parseTwelveHour("5:00PM")
    assert(timeOfday.isDefined)
    assert(timeOfday.get == new LocalTime(17,0,0))
  }


  @Test
  def timeParsing() {

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

    val data = Map(
      "what"->"build this app",
      "every"->"every week",
      "to"->"to",
      "at"->"at 5:00PM",
      "on"->"on Wednesday",
      "time"->"5:00PM",
      "repeat"->"week",
      "when"->"Wednesday")

    assertParsedProperly(mention, data)
  }


  @Test def parsingA() {
    val mention = "@RemindTweets Remind me to eat lunch in 4 hours"

    val data = Map("what"->"eat lunch", "relativeTime"->"4 hours")
    val missingKeys = List("repeat")
    assertParsedProperly(mention, data, missingKeys)
  }


  @Test def parsingB() {
    val mention = "@RemindTweets Remind me to get dinner at 5:40pm."

    val data = Map("what"->"get dinner", "time"->"5:40pm")
    val missingKeys = List("repeat")
    assertParsedProperly(mention, data, missingKeys)
  }


  @Test def parsingC() {
    val mention = " @remindtweets Remind me to get chips at the store at 6:15pm."

    val data = Map("what"->"get chips at the store", "time"->"6:15pm")
    val missingKeys = List("repeat")
    assertParsedProperly(mention, data, missingKeys)
  }


  @Test def parsingD() {

    val mention = "@remindtweets Remind me to build this app on Tomorrow at 6:00 PM"

    val data = Map("what"->"build this app", "time"->"6:00 PM", "when"->"Tomorrow")
    val missingKeys = List("repeat")
    assertParsedProperly(mention, data, missingKeys)
  }


  @Test def parsingE() {

    val mention = "@remindtweets Remind me to build this app on Wednesday at 6:00PM every week"

    val data = Map("what"->"build this app", "time"->"6:00PM", "when"->"Wednesday", "repeat"->"week")
    assertParsedProperly(mention, data)
  }


  @Test def parsingF() {

    val mention = "@remindtweets Remind me to get dessert in 4 hours and 15 minutes"

    val data = Map("what"->"get dessert", "relativeTime"->"4 hours and 15 minutes")
    assertParsedProperly(mention, data, List("repeat"))
  }


  @Test def parseRepeat() {

    val repeatTest = Map(
      "week"->Repeat.Weekly,
      "monthly"->Repeat.Monthly,
      "day"->Repeat.Daily,
      "hour"->Repeat.EveryHour)

    for ((key, value) <- repeatTest) {
      val x = ReminderParsing.getRepeatFrequency(Some(key))
      assert(x === value)
    }
  }

}