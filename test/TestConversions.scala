
import models.Reminder
import org.specs2.mutable._
import org.joda.time.LocalDateTime
import helpers.ReminderCreation.handleStatus


class TestConversions extends Specification {


  "String To Json A" in {
    val json: org.json4s.JValue = helpers.Converters.getJsonFromString(helpers.Converters.dummyJsonA)
    json\\ "screenName" must equalTo("HerbieLewis")
  }

  "String To Json B" in {
    val json: org.json4s.JValue = helpers.Converters.getJsonFromString(helpers.Converters.dummyJsonB)
    (json\\("screenName")).asInstanceOf[String] must equalTo("DUMMY")
  }

  "String To Status" in {
    val status: twitter4j.Status = helpers.Converters.createStatusFromJsonString(helpers.Converters.dummyJsonA)
    status.getUser.getScreenName must equalTo("HerbieLewis")
  }

}
